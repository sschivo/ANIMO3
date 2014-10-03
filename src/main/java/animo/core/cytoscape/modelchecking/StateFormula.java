package animo.core.cytoscape.modelchecking;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import animo.core.cytoscape.Animo;
import animo.core.model.Model;

public class StateFormula extends JPanel
{
    private class ReactantId implements Comparable<ReactantId>
    {
        private String alias;
        private String identifier;
        private CyNode node;

        public ReactantId(String alias, String identifier, CyNode node)
        {
            this.alias = alias;
            this.identifier = identifier;
            this.node = node;
        }

        @Override
        public int compareTo(ReactantId o)
        {
            return this.alias.compareTo(o.alias);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;

            ReactantId r = (ReactantId) obj;
            return alias.equals(r.alias);
        }

        public String getAlias()
        {
            return alias;
        }

        public String getIdentifier()
        {
            return identifier;
        }

        public CyNode getNode()
        {
            return node;
        }

        @Override
        public int hashCode()
        {
            return alias.hashCode();
        }

        @Override
        public String toString()
        {
            return getAlias();
        }
    }

    private static final long serialVersionUID = -7020762010666811781L;
    public static char REACTANT_NAME_DELIMITER = '@';
    private ReactantId[] reactants;
    private JComboBox<ReactantId> combo1;
    private JComboBox<BoundType> combo2;
    private JLabel boundValue;
    private JSlider slider;

    private StateFormula selectedFormula = null;

    @SuppressWarnings("unchecked")
    public StateFormula()
    {

        Box content = new Box(BoxLayout.X_AXIS);
        //Read the list of reactant identifiers and aliases from the nodes in the current network
        CyApplicationManager cyApplicationManager = Animo.getCytoscapeApp().getCyApplicationManager();
        final CyNetwork network = cyApplicationManager.getCurrentNetwork();
        //reactantAliases = new String[network.getNodeCount()];
        //reactantIdentifiers = new String[network.getNodeCount()];
        List<ReactantId> reactantsV = new ArrayList<ReactantId>();
        Iterator<CyNode> nodes = network.getNodeList().iterator();
        //for (int i = 0; nodes.hasNext(); i++) {
        while (nodes.hasNext())
        {
            CyNode node = nodes.next();
            String enabledString = network.getRow(node).get(Model.Properties.ENABLED, String.class);
            Boolean enabledBool = network.getRow(node).get(Model.Properties.ENABLED, Boolean.class);
            if (enabledString != null && !enabledBool)
            { //Don't allow disabled nodes to be used in the formula
                continue;
            }
            //reactantIdentifiers[i] = node.getIdentifier();
            String identif = node.getSUID().toString();
            String alias = network.getRow(node).get(Model.Properties.CANONICAL_NAME, String.class);
            if (alias == null)
            {
                alias = identif;
            }
            reactantsV.add(new ReactantId(alias, identif, node));
        }
        Collections.sort(reactantsV);
        reactants = reactantsV.toArray(new ReactantId[] {});
        combo1 = new JComboBox<ReactantId>(reactants);
        combo1.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                ReactantId reactant = (ReactantId) combo1.getSelectedItem();
                int nLevels = network.getRow(reactant.getNode()).get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
                if (slider != null)
                {
                    slider.setMaximum(nLevels);
                    int space = (slider.getMaximum() - slider.getMinimum() + 1) / 5;
                    if (space < 1)
                        space = 1;
                    slider.setMajorTickSpacing(space);
                    slider.setMinorTickSpacing(space / 2);
                    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
                    for (int i = slider.getMinimum(); i <= slider.getMaximum(); i += space)
                    {
                        labelTable.put(i, new JLabel("" + i));
                    }
                    slider.setLabelTable(labelTable);
                    int init = network.getRow(reactant.getNode()).get(Model.Properties.INITIAL_LEVEL, Integer.class);
                    slider.setValue(init);
                }
            }

        });
        BoundType[] bounds = new BoundType[] { BoundType.LT, BoundType.LE, BoundType.EQ, BoundType.GE, BoundType.GT };
        combo2 = new JComboBox<BoundType>(bounds);
        boundValue = new JLabel("0");
        slider = new JSlider(0, 100);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(20);
        slider.addChangeListener(new ChangeListener()
        {

            @Override
            public void stateChanged(ChangeEvent e)
            {
                boundValue.setText("" + slider.getValue());
            }

        });
        content.add(combo1);
        content.add(combo2);
        content.add(boundValue);
        //content.add(slider);
        Box contentV = new Box(BoxLayout.Y_AXIS);
        contentV.add(content);
        contentV.add(slider);
        combo1.setSelectedIndex(0);
        this.add(contentV);
        slider.setValue(slider.getMaximum() / 2);
        boundValue.setText("" + slider.getValue());
    }

    @Override
    public void addMouseListener(MouseListener ml)
    {
        super.addMouseListener(ml);
        combo1.addMouseListener(ml);
        combo2.addMouseListener(ml);
        slider.addMouseListener(ml);
    }

    public StateFormula getSelectedFormula()
    {
        if (selectedFormula == null)
        {
            //String selectedID = REACTANT_NAME_DELIMITER + reactantIdentifiers[combo1.getSelectedIndex()] + REACTANT_NAME_DELIMITER;
            ReactantId reactant = (ReactantId) combo1.getSelectedItem();
            selectedFormula = new ActivityBound(new ReactantName(reactant.getIdentifier(), reactant.getAlias()), (BoundType) combo2.getSelectedItem(), ""
                    + slider.getValue());
        }
        return selectedFormula;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        combo1.setEnabled(enabled);
        combo2.setEnabled(enabled);
        slider.setEnabled(enabled);
    }

    public void setReactantIDs(Model m)
    {
        getSelectedFormula().setReactantIDs(m);
    }

    public boolean supportsPriorities()
    {
        return getSelectedFormula().supportsPriorities();
    }

    public String toHumanReadable()
    {
        return getSelectedFormula().toHumanReadable();
    }

    @Override
    public String toString()
    {
        return getSelectedFormula().toString();
    }
}
