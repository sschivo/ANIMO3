/**
 * 
 */
package animo.inat.model;

import java.io.Serializable;

/**
 * A vertex is a component in the network.
 * 
 * @author B. Wanders
 */
public class Reactant extends Entity implements Serializable
{
    public enum MoleculeType
    {
        CYTOKINE, RECEPTOR, KINASE, PHOSPHATASE, OTHER, MRNA, GENE, DUMMY;
    }

    private MoleculeType type;

    private static final long serialVersionUID = -8610211944385660028L;

    public Reactant()
    {
    }

    /**
     * Constructor.
     * 
     * @param id the vertex id
     */
    public Reactant(String id)
    {
        super(id);
    }

    /**
     * returns a (deep) copy of this
     * @return
     */
    public Reactant copy()
    {
        Reactant e = new Reactant(this.id);
        e.setType(type);
        e.setModel(this.getModel());
        e.properties = this.properties.copy();
        return e;
    }


    public boolean getEnabled()
    {
        return this.get(Model.Properties.ENABLED).as(Boolean.class);
    }

    public String getName()
    {
        return get(Model.Properties.ALIAS).as(String.class);
    }

    public MoleculeType getType()
    {
        String pb = (String) properties.get(Model.Properties.MOLECULE_TYPE).getValue();
        if (!type.name().equals(pb))
            type = MoleculeType.valueOf(pb);
        return type;
    }

    public void setName(String name)
    {
        this.let(Model.Properties.ALIAS).be(name);
    }

    public void setType(MoleculeType type)
    {
        this.type = type;
        if (type != null)
        {
            properties.let(Model.Properties.MOLECULE_TYPE).be(type.toString());
        }
    }

    @Override
    public String toString()
    {
        return "Reactant '" + this.getId() + "'";
    }
}
