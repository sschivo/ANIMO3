package animo.core.analyser.uppaal;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;

public class VariablesModelODE extends VariablesModel {

	private static final String REACTANT_INDEX = Model.Properties.REACTANT_INDEX,
								OUTPUT_REACTANT = Model.Properties.OUTPUT_REACTANT,
								SCENARIO = Model.Properties.SCENARIO,
								HAS_INFLUENCING_REACTIONS = "has influencing reactions";
	private boolean normalModelChecking = false,
					useOldResetting = true,
					dAlternating = false;
	private int uncertainty = 0;
	@Override
	protected void appendModel(StringBuilder out, Model m) {
		out.append("<?xml version='1.0' encoding='utf-8'?>");
		out.append(newLine);
		out.append("<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_1.dtd'>");
		out.append(newLine);
		out.append("<nta>");
		out.append(newLine);
		out.append("<declaration>");
		out.append(newLine);
		
		// output global declarations
		out.append("// Place global declarations here.");
		out.append(newLine);
		out.append("clock globalTime;");
		out.append(newLine);
//		out.append("const int INFINITE_TIME = " + INFINITE_TIME + ";");
//		out.append(newLine);
//		int countReactants = 0;
//		for (Reactant r : m.getReactantCollection()) {
//			if (r.get(ENABLED).as(Boolean.class)) {
//				countReactants++;
//			}
//		}
//		out.append("const int N_REACTANTS = " + countReactants + ";");
//		out.append(newLine);
//		out.append("broadcast chan reacting[N_REACTANTS];");
//		out.append(newLine);
		
		out.append(newLine);
		
		int reactantIndex = 0;
		for (Reactant r : m.getSortedReactantList()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let(REACTANT_INDEX).be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		
		
		for (Reaction r : m.getReactionCollection()) { //The time tables (filled by the rates, not times)
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionTables(out, m, r);
		}
		
		
		out.append("</declaration>");
		
		out.append(newLine);
		out.append(newLine);
		// output templates
		this.appendTemplates(out, m);
		
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		for (Reactant r : m.getSortedReactantList()) {
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			this.appendReactionProcess(out, m, r, reactantIndex);
		}
		
		out.append(newLine);
		out.append(newLine);
		out.append(newLine);
		
		// compose the system
		out.append("system ");
		Iterator<Reactant> iter = m.getSortedReactantList().iterator();
		boolean first = true;
		while (iter.hasNext()) {
			Reactant r = iter.next();
			if (!r.get(ENABLED).as(Boolean.class) || !r.get(HAS_INFLUENCING_REACTIONS).as(Boolean.class)) continue;
			if (!first) {
				out.append(", ");
			}
			out.append(r.getId() + "_");
			first = false;
		}
		out.append(";");
		
		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}
	
	private void appendReactionProcess(StringBuilder out, Model m, Reactant r, int index) {
		out.append(r.getId() + "_ = Reactant_" + r.getId() + "(" + r.getId() + ", " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ");");
		out.append(newLine);
	}
	
	private void appendReactionTables(StringBuilder out, Model m, Reaction r) {
		String r1Id = r.get(CATALYST).as(String.class);
		String r2Id = r.get(REACTANT).as(String.class);
		String rOutput = r.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get(Model.Properties.ALIAS).as(String.class) + ") " + (!r2Id.equals(rOutput)?("AND " + r2Id + " (" + m.getReactant(r2Id).get(Model.Properties.ALIAS).as(String.class) + ") "):"") + (r.get(INCREMENT).as(Integer.class)>0?"-->":"--|") + " " + (r2Id.equals(rOutput)?(r2Id + " (" + m.getReactant(r2Id).get(Model.Properties.ALIAS).as(String.class) + ")"):(rOutput + " (" + m.getReactant(rOutput).get(Model.Properties.ALIAS).as(String.class) + ")")));
		//out.append(newLine);
		
		//out.append("timeActivity " + r.getId() + ";");
		out.append(newLine);
		out.append("const double k_" + r.getId() + " = " + (r.get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class) / (m.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class) * r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class))) + ";");
		out.append(newLine);
		out.append(newLine);
		
	}


	@Override
	protected void appendTemplates(StringBuilder out, Model m) {
		try {
			StringWriter outString;
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StringBuilder template = new StringBuilder("<template><name x=\"5\" y=\"5\">ODE_Model</name><declaration>");
			
			
			for (Reactant r : m.getSortedReactantList()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
//				outString = new StringWriter();
				Vector<Reaction> influencingReactions = new Vector<Reaction>();
				for (Reaction re : m.getReactionCollection()) {
					if (re.get(OUTPUT_REACTANT).as(String.class).equals(r.getId()))  { //If the reactant is downstream of a reaction, count that reaction
						influencingReactions.add(re);
					}
				}
				
				if (influencingReactions.size() < 1) {
					r.let(HAS_INFLUENCING_REACTIONS).be(false);
//					continue;
				}
				r.let(HAS_INFLUENCING_REACTIONS).be(true);
				
//				StringBuilder template = new StringBuilder("<template><name>Reactant_" + r.getId() + "</name><parameter>int&amp; R, const int MAX</parameter><declaration>int[-1, 1] delta" + (dAlternating?", deltaNew = 0, deltaOld = 0, deltaOldOld = 0, deltaOldOldOld = 0;\nbool deltaAlternating = false":"") + ";\ntime_t tL, tU;\nclock c;\ndouble_t totalRate;\n\n\n" + (dAlternating?"void updateDeltaOld() {\n\tdeltaOldOldOld = deltaOldOld;\n\tdeltaOldOld = deltaOld;\n\tdeltaOld = deltaNew;\n\tdeltaNew = delta;\n\tdeltaAlternating = false;\n\tif (deltaOldOldOld != 0) { //We have updated delta at least 4 times, so we can see whether we have an oscillation\n\t\tif (deltaNew == deltaOldOld &amp;&amp; deltaOld == deltaOldOldOld &amp;&amp; deltaNew != deltaOld) { //Pairwise equal and alternating (e.g. +1, -1, +1, -1): we are oscillating\n\t\t\tdeltaAlternating = true;\n\t\t\tdeltaNew = deltaOld = deltaOldOld = deltaOldOldOld = 0;\n\t\t}\n\t}\n}\n\n":"") + "void update() {\n");
				for (Reaction re : influencingReactions) {
					int scenario = re.get(SCENARIO).as(Integer.class);
					boolean activeR1, activeR2;
					if (scenario == 0 || scenario == 1) {
						activeR1 = true;
						if (re.get(INCREMENT).as(Integer.class) >= 0) {
							activeR2 = false;
						} else {
							activeR2 = true;
						}
					} else if (scenario == 2) {
						activeR1 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1).as(Boolean.class);
						activeR2 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2).as(Boolean.class);
					} else {
						//TODO: this should never happen, because we have already made these checks
						activeR1 = activeR2 = true;
					}
					switch (scenario) {
						case 0:
							template.append("\tdouble_t " + re.getId() + "_r = scenario1(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						case 1: case 2:
							template.append("\tdouble_t " + re.getId() + "_r = scenario2_3(k_" + re.getId() + ", int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId() + "Levels), " + activeR2 + ", int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "), int_to_double(" + m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId() + "Levels), " + activeR1 + ");\n");
							break;
						default:
							break;
					}
				}
				
				for (Reaction re : influencingReactions) { //Now we put the code to check that we are not on a border case. In such case, the downstream reactant (called "R" in this automaton template) is also involved in the computation of the reaction rate (so we look at scenario 2 or 3), and the value of R is the only reason why the reaction is not performed (the other influencing reactant is fine). In that case, we want to keep considering the reaction as active, to (approximatively: in truth we should deal with infinitesimals) counterbalance any other possible reaction that may otherwise define the total rate like it was the only one actually present. We want to avoid performing a step with significant imprecision because of this.
					int scenario = re.get(SCENARIO).as(Integer.class);
					boolean activeR1, activeR2;
					if (scenario == 0 || scenario == 1) {
						activeR1 = true;
						if (re.get(INCREMENT).as(Integer.class) >= 0) {
							activeR2 = false;
						} else {
							activeR2 = true;
						}
					} else if (scenario == 2) {
						activeR1 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1).as(Boolean.class);
						activeR2 = re.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2).as(Boolean.class);
					} else {
						//TODO: this should never happen, because we have already made these checks
						activeR1 = activeR2 = true;
					}
					switch (scenario) {
						case 0:
							//Nothing to worry about
							break;
						case 1: case 2:
							String reactionID = re.getId(),
								   r2ID = m.getReactant(re.get(Model.Properties.REACTANT).as(String.class)).getId(),
								   r1ID = m.getReactant(re.get(Model.Properties.CATALYST).as(String.class)).getId(),
								   rOutputID = m.getReactant(re.get(Model.Properties.OUTPUT_REACTANT).as(String.class)).getId();
							String rNotOutputID; //The reactant that needs to be working (e.g. if it is r1, it must be active if r1Active is true) while the downstream is at the limit (that's the only case we want to consider: when the reaction rate results to be 0 because the reaction is already "done" but if there was more substrate it could still occur)
							boolean activityNotOutput;
							if (rOutputID.equals(r1ID)) {
								rNotOutputID = r2ID;
								activityNotOutput = activeR2;
							} else {
								rNotOutputID = r1ID;
								activityNotOutput = activeR1;
							}
							//Check whether the downstream reactant is involved in the computation: in that case, we want to "declare" the rate as we were still one step from completing the reaction
							if (rOutputID.equals(r1ID) || rOutputID.equals(r2ID)) {
								template.append("\tif (" + reactionID + "_r.b == 0 &amp;&amp; " + rNotOutputID + (activityNotOutput?" > 0":" < " + rNotOutputID + "Levels") + ") { //If the downstream reactant (R) is the only reason for the inactivity of the reaction " + reactionID + " (note that R is actually " + rOutputID + "), consider the reaction as still available (recomputing the rate as the one closest to the current situation), to avoid significant errors in performing the next update to R. We want to avoid considering any other reaction active on R as the only one possible during the whole next delta increment to R.\n\t\t//Using the following rate is not very precise, but makes for a much more precise total rate than we would get if considering the reaction as inactive (especially with low levels of granularity for R, i.e. small values of MAX)\n\t\t" + reactionID + "_r = scenario2_3(k_" + reactionID + ", int_to_double(" + ((r2ID.equals(rOutputID) && activeR2)?"1":((r2ID.equals(rOutputID) && !activeR2)?r2ID + "Levels - 1":r2ID)) + "), int_to_double(" + r2ID + "Levels), " + activeR2 + ", int_to_double(" + ((r1ID.equals(rOutputID) && activeR1)?"1":((r1ID.equals(rOutputID) && !activeR1)?r1ID + "Levels - 1":r1ID)) + "), int_to_double(" + r1ID + "Levels), " + activeR1 + ");\n\t}\n");
							}
							break;
						default:
							break;
					}
				}
				
				//Finally compute the total rate
				template.append("\ttotalRate = ");
				if (influencingReactions.size() == 1) {
					Reaction re = influencingReactions.get(0);
					int increment = re.get(INCREMENT).as(Integer.class);
					String subtr1 = (increment > 0)?"":"subtract(zero, ",
						   subtr2 = (increment > 0)?"":")";
					template.append(subtr1 + re.getId() + "_r" + subtr2 + ";\n");
					if (uncertainty != 0) {
						template.append("\t" + re.getId() + ".T = round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
					} else {
						template.append("\t" + re.getId() + ".T = round(inverse(" + re.getId() + "_r));\n");
					}
				} else {
					StringBuilder computation = new StringBuilder("zero");
					for (Reaction re : influencingReactions) {
						computation.append(", " + re.getId() + "_r)");
						if (re.get(Model.Properties.INCREMENT).as(Integer.class) > 0) {
							computation.insert(0, "add(");
						} else {
							computation.insert(0, "subtract(");
						}
					}
					template.append(computation);
					template.append(";\n");
					for (Reaction re : influencingReactions) {
						if (uncertainty != 0) {
							template.append("\t" + re.getId() + ".T = " + "round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
						} else {
							template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_r));\n");
						}
					}
				}
				template.append("\tif (totalRate.b &lt; 0) {\n\t\tdelta = -1;\n\t\ttotalRate.b = -totalRate.b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (totalRate.b != 0) {\n");
				if (uncertainty != 0) {
					template.append("\t\ttL = round(multiply(inverse(totalRate), LOWER_UNC));\n\t\ttU = round(multiply(inverse(totalRate), UPPER_UNC));\n");
				} else {
					template.append("\t\ttL = round(inverse(totalRate));\n\t\ttU = tL;\n");
				}
				
				template.append("\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n\tif (tL != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so large, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\nvoid react() {\n\tif (0 &lt;= R + delta &amp;&amp; R + delta &lt;= MAX) {\n\t\tR = R + delta;\n\t}\n" + (dAlternating?"\tupdateDeltaOld();\n":"") + "\tupdate();\n}\n\nbool can_react() {\n\treturn " + (dAlternating?"!deltaAlternating &amp;&amp; ":"") + "(tL != INFINITE_TIME &amp;&amp; ((delta &gt;= 0 &amp;&amp; R &lt; MAX) || (delta &lt; 0 &amp;&amp; R &gt; 0)));\n}\n\nbool cant_react() {\n\treturn " + (dAlternating?"deltaAlternating || ":"") + "(tL == INFINITE_TIME || (delta &gt;= 0 &amp;&amp; R == MAX) || (delta &lt; 0 &amp;&amp; R == 0));\n}</declaration>");
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location><location id=\"id1\" x=\"-1528\" y=\"-728\"><committed/></location><location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location><location id=\"id7\" x=\"-1552\" y=\"-856\"><name x=\"-1656\" y=\"-872\">not_reacting</name>" + (useOldResetting?"":"<label kind=\"invariant\" x=\"-1656\" y=\"-856\">c'==0</label>") + "</location><location id=\"id8\" x=\"-1416\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location><location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-720\">c &lt;= tU\n|| tU ==\nINFINITE_TIME</label></location><init ref=\"id6\"/><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1640\" y=\"-760\">tU == INFINITE_TIME\n|| c &lt;= tU</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1640\" y=\"-776\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "</transition><transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-712\">tU != INFINITE_TIME\n&amp;&amp; c &gt; tU</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1608\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1608\" y=\"-680\">c := tU</label><nail x=\"-1528\" y=\"-680\"/><nail x=\"-1608\" y=\"-680\"/></transition><transition><source ref=\"id0\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1816\" y=\"-632\">c &lt; tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-600\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1816\" y=\"-616\">update()</label><nail x=\"-1848\" y=\"-616\"/><nail x=\"-1464\" y=\"-616\"/></transition><transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1816\" y=\"-680\">c &gt;= tL</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1816\" y=\"-664\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition><transition><source ref=\"id6\"/><target ref=\"id8\"/>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1344\" y=\"-744\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
				int y1 = -904,
					y2 = -888,
					y3 = -848,
					incrY = -40;
				Vector<Reactant> alreadyOutputReactants = new Vector<Reactant>(); //Keep track of reactants that already have a transition to avoid input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from not_reacting to updating
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1512\" y=\"-840\">cant_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1512\" y=\"-856\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "<nail x=\"-1416\" y=\"-824\"/><nail x=\"-1552\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1512\" y=\"-744\">can_react()</label>" + (normalModelChecking?"<label kind=\"synchronisation\" x=\"-1512\" y=\"-728\">sequencer[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label>":"") + "</transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1576\" y=\"-816\">c &gt;= tL</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + r.get(REACTANT_INDEX).as(Integer.class) + "]!</label><label kind=\"assignment\" x=\"-1568\" y=\"-784\">react(), c := 0</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
				y1 = -744;
				y2 = -728;
				incrY = -48;
				alreadyOutputReactants = new Vector<Reactant>(); //Keep trace of which reactants already have a transition for them, because otherwise we get input nondeterminism
				for (Reaction re : influencingReactions) { //Transitions from waiting to stubborn
					int scenario = re.get(SCENARIO).as(Integer.class);
					Reactant catalyst = m.getReactant(re.get(CATALYST).as(String.class)),
							 reactant = m.getReactant(re.get(REACTANT).as(String.class)); //This is not null only when scenario != 0
					switch (scenario) {
						case 0:
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id9\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1832\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><nail x=\"-1752\" y=\"" + y2 + "\"/><nail x=\"-1840\" y=\"" + y2 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("</template>");
//				document = documentBuilder.parse(new ByteArrayInputStream(template.toString().getBytes()));
//				tra.transform(new DOMSource(document), new StreamResult(outString));
//				out.append(outString.toString());
//				out.append(newLine);
//				out.append(newLine);
			}
			
			document = documentBuilder.parse(new ByteArrayInputStream(template.toString().getBytes()));
			outString = new StringWriter();
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);

		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
	
	protected String getReactionName(Reaction r) {
		return r.getId(); //The (UPPAAL) ID of the reaction is already set when we create it in the model
	}
	
	@Override
	protected void appendReactantVariables(StringBuilder out, Reactant r) {
		// outputs the global variables necessary for the given reactant
		out.append("//" + r.getId() + " = " + r.get(Model.Properties.ALIAS).as(String.class));
		out.append(newLine);
		out.append("clock " + r.getId() + ";"); //In the ODE model of UPPAAL, clocks are actually the variables
		out.append(newLine);
		out.append("const int " + r.getId() + "Init = " + r.get(INITIAL_LEVEL).as(Integer.class) + ";");
		out.append(newLine);
		out.append("const int " + r.getId() + "Levels = " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ";");
		out.append(newLine);
		out.append(newLine);
	}
	
	@Override
	protected String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "{0, 0}";
		} else {
			return formatDouble(1.0 / time); //time is guaranteed not to be 0, because we use 0 as a signal that rounding is not good enough, and increase time scale
		}
	}
	
	private String formatDouble(double d) {
		int b, e; 
		e = (int)Math.round(Math.log10(d)) - 3;
		b = (int)Math.round(d * Math.pow(10, -e));
		if (b < 10) { //We always want 4 figures
			b = b * 1000;
			e = e - 3;
		} else if (b < 100) {
			b = b * 100;
			e = e - 2;
		} else if (b < 1000) {
			b = b * 10;
			e = e - 1;
		}
		return "{" + b + ", " + e + "}";
	}

}
