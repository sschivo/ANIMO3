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

import animo.core.AnimoBackend;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.util.XmlConfiguration;

public class VariablesModelReactantCenteredMorePreciseNew3 extends VariablesModel {

	private static final String REACTANT_INDEX = Model.Properties.REACTANT_INDEX,
								OUTPUT_REACTANT = Model.Properties.OUTPUT_REACTANT,
								SCENARIO = Model.Properties.SCENARIO,
								HAS_INFLUENCING_REACTIONS = "has influencing reactions";
	private boolean useOldResetting = true;
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
		out.append("const int INFINITE_TIME = " + INFINITE_TIME + ";");
		out.append(newLine);
		int countReactants = 0;
		for (Reactant r : m.getReactantCollection()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				countReactants++;
			}
		}
		out.append("const int N_REACTANTS = " + countReactants + ";");
		out.append(newLine);
		out.append("broadcast chan reacting[N_REACTANTS];");
		out.append(newLine);
		out.append("clock c[N_REACTANTS];");
		out.append(newLine);
				
		useOldResetting = true;
		if (m.getProperties().has("useOldResetting") && !m.getProperties().get("useOldResetting").as(Boolean.class)) {
			useOldResetting = false;
		}
		
		//TODO Not only are channel priorities unsupported by the statistical model checking engine, but they also appear to reduce performances (!) of the normal engine
		if (m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE) != null && m.getProperties().get(Model.Properties.MODEL_CHECKING_TYPE).as(Integer.class) == Model.Properties.NORMAL_MODEL_CHECKING) {
			useOldResetting = true;
		}
//			normalModelChecking = true;
//			out.append("broadcast chan sequencer[N_REACTANTS];");
//			out.append(newLine);
//			if (countReactants > 1) {
//				out.append("chan priority ");
//				for (int i=0;i<countReactants-1;i++) {
//					out.append("reacting[" + i + "] &lt; ");
//				}
//				out.append("reacting[" + (countReactants-1) + "];");
//				out.append(newLine);
//				out.append("chan priority ");
//				for (int i=0;i<countReactants-1;i++) {
//					out.append("sequencer[" + i + "] &lt; ");
//				}
//				out.append("sequencer[" + (countReactants-1) + "];");
//				out.append(newLine);
//			}
//		} else {
//			normalModelChecking = false;
//		}
		out.append(newLine);
		
		int reactantIndex = 0;
		for (Reactant r : m.getSortedReactantList()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let(REACTANT_INDEX).be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		
		
		//The encoding of double numbers with integer variables (exponential notation with 3 significant figures)
		out.append(newLine);
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\tint[-99980001, 99980001] b;"); //99980001 = 9999 * 9999, i.e. the maximum result of a multiplication between two .b of double numbers
		out.append(newLine);
		out.append("\tint e;");
		out.append(newLine);
		out.append("} double_t;");
		out.append(newLine);
		out.append(newLine);
		out.append("const double_t zero = {0, 0};");
		out.append(newLine);
		out.append("const double_t INFINITE_TIME_DOUBLE = {-1000, -3}, //INFINITE_TIME (-1) translated into double");
		out.append(newLine);
		out.append("\t\t\t   TWO = {2000, -3}; // 2");
		out.append(newLine);
		uncertainty = 0;
		XmlConfiguration configuration = AnimoBackend.get().configuration();
		String uncertaintyStr = configuration.get(XmlConfiguration.UNCERTAINTY_KEY, null);
		if (uncertaintyStr != null) {
			try {
				uncertainty = Integer.parseInt(uncertaintyStr);
			} catch (Exception ex) {
				uncertainty = 0;
			}
		} else {
			uncertainty = 0;
		}
		if (uncertainty != 0) {
			double lowerUnc = (100.0 - uncertainty) / 100.0,
				   upperUnc = (100.0 + uncertainty) / 100.0;
			out.append("const double_t LOWER_UNC = " + formatDouble(lowerUnc) + ", //Lower and upper scale factors to apply uncertainty. E.g. for +/- 5% uncertainty, we have lower uncertainty = 0.95, upper uncertainty = 1.05");
			out.append(newLine);
			out.append("             UPPER_UNC = " + formatDouble(upperUnc) + ";");
			out.append(newLine);
		}
		out.append(newLine);
		out.append("typedef int[-1, 1073741822] time_t;"); //The type for time values
		out.append(newLine);
		out.append(newLine);
		//In order to still show the reaction activity ratio we use the original time bounds
		out.append("typedef struct {");
		out.append(newLine);
		out.append("\ttime_t T;");
		out.append(newLine);
		out.append("} timeActivity;");
		out.append(newLine);
		out.append(newLine);
		
		for (Reaction r : m.getReactionCollection()) { //The time tables (filled by the rates, not times)
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionTables(out, m, r);
		}
		out.append(newLine);
		out.append(newLine);
		out.append("double_t subtract(double_t a, double_t b) { // a - b"); // Subtraction
		out.append(newLine);
		out.append("\tdouble_t r = {-1000, -1000};");
		out.append(newLine);
		out.append("\tif (a.b == 0) {");
		out.append(newLine);
		out.append("\t\tr.b = -b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t\treturn r;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn a;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a.e - b.e) &gt;= 4) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 4) {");
		out.append(newLine);
		out.append("\t\tr.b = -b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t\treturn r;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == b.e) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/10;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/100;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b - b.b/1000;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/10 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/100 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/1000 - b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t add(double_t a, double_t b) { // a + b"); // Addition
		out.append(newLine);
		out.append("\tdouble_t r = {-1000,-1000};");
		out.append(newLine);
		out.append("\tif (a.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn b;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.b == 0) {");
		out.append(newLine);
		out.append("\t\treturn a;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((a.e - b.e) &gt;= 4) return a;");
		out.append(newLine);
		out.append("\tif ((b.e - a.e) &gt;= 4) return b;");
		out.append(newLine);
		out.append("\tif (a.e == b.e) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/10;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/100;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e - b.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b + b.b/1000;");
		out.append(newLine);
		out.append("\t\tr.e = a.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 1) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/10 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 2) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/100 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (b.e - a.e == 3) {");
		out.append(newLine);
		out.append("\t\tr.b = a.b/1000 + b.b;");
		out.append(newLine);
		out.append("\t\tr.e = b.e;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t multiply(double_t a, double_t b) { // a * b"); // Multiplication
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tr.b = a.b * b.b;");
		out.append(newLine);
		out.append("\tif (r.b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b / 1000;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tr.b = 1 + r.b / 1000;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tr.e = a.e + b.e + 3;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t} else if (r.b &gt; 9999 || r.b &lt; -9999) {");
		out.append(newLine);
        out.append("\t\tr.b = r.b / 10;");
		out.append(newLine);
        out.append("\t\tr.e = r.e + 1;");
		out.append(newLine);
    	out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t inverse(double_t a) { // 1 / a"); // Inverse
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tif (a.b == 0 || a.e &lt; -9) { // 1 / 1e-9 is still ok, but 1 / 1e-10 is too large (&gt; 2&#94;30 - 2, the largest allowed constant for guards/invariants)");
		out.append(newLine);
		out.append("\t\treturn INFINITE_TIME_DOUBLE;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tr.b = 1000000 / a.b;");
		out.append(newLine);
		out.append("\tr.e = -6 - a.e;");
		out.append(newLine);
		out.append("\tif ((r.b &gt; 0 &amp;&amp; r.b &lt; 10) || (r.b &lt; 0 &amp;&amp; r.b &gt; -10)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 3;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 100) || (r.b &lt; 0 &amp;&amp; r.b &gt; -100)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 100;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 2;");
		out.append(newLine);
		out.append("\t} else if ((r.b &gt; 0 &amp;&amp; r.b &lt; 1000) || (r.b &lt; 0 &amp;&amp; r.b &gt; -1000)) {");
		out.append(newLine);
		out.append("\t\tr.b = r.b * 10;");
		out.append(newLine);
		out.append("\t\tr.e = r.e - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("time_t power(int a, int b) { // a ^ b (b &gt;= 0)"); // Integer power
		out.append(newLine);
		out.append("\ttime_t r = 1;");
		out.append(newLine);
		out.append("\twhile (b &gt; 0) {");
		out.append(newLine);
		out.append("\t\tr = r * a;");
		out.append(newLine);
		out.append("\t\tb = b - 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("time_t round(double_t a) { // double --&gt; integer"); // Round
		out.append(newLine);
		out.append("\tif (a == INFINITE_TIME_DOUBLE) { // Don't need to translate literally if we have infinite");
		out.append(newLine);
		out.append("\t\treturn INFINITE_TIME;");
		out.append(newLine);
	    out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e &lt; -3) {");
		out.append(newLine);
		out.append("\t\tif (a.b &lt; 5000) return 0;");
		out.append(newLine);
		out.append("\t\telse return 1;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -1) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 10 &lt; 5) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 10;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 10;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -2) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 100 &lt; 50) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 100;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 100;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (a.e == -3) {");
		out.append(newLine);
		out.append("\t\tif (a.b % 1000 &lt; 500) {");
		out.append(newLine);
		out.append("\t\t\treturn a.b / 1000;");
		out.append(newLine);
		out.append("\t\t} else {");
		out.append(newLine);
		out.append("\t\t\treturn 1 + a.b / 1000;");
		out.append(newLine);
		out.append("\t\t}");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn a.b * power(10, a.e);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append(newLine);
		out.append("double_t scenario1(double_t k, double_t r1, double_t r1Levels, bool r1Active) {"); // Scenario 1
		out.append(newLine);
		out.append("\tdouble_t E;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE = r1;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\tE = subtract(r1Levels, r1);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn multiply(k, E);");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t scenario2_3(double_t k, double_t r2, double_t r2Levels, bool r2Active, double_t r1, double_t r1Levels, bool r1Active) {"); // Scenarios 2 and 3
		out.append(newLine);
		out.append("\tdouble_t E, S;");
		out.append(newLine);
		out.append("\tif (r1Active) { //If we depend on active R1, the level of activity is the value of E");
		out.append(newLine);
		out.append("\t\tE = r1;");
		out.append(newLine);
		out.append("\t} else { //otherwise we find the inactivity level via the total number of levels");
		out.append(newLine);
		out.append("\t\tE = subtract(r1Levels, r1);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\tif (r2Active) { //Same for R2");
		out.append(newLine);
		out.append("\t\tS = r2;");
		out.append(newLine);
		out.append("\t} else {");
		out.append(newLine);
		out.append("\t\tS = subtract(r2Levels, r2);");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn multiply(k, multiply(E, S));");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("");
		out.append(newLine);
		out.append("double_t int_to_double(int a) { //Used to translate an activity level into double."); // Translate an int to double
		out.append(newLine);
		out.append("\tdouble_t r;");
		out.append(newLine);
		out.append("\tif (a &lt; 10) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 1000;");
		out.append(newLine);
		out.append("\t\tr.e = -3;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 100) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 100;");
		out.append(newLine);
		out.append("\t\tr.e = -2;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 1000) {");
		out.append(newLine);
		out.append("\t\tr.b = a * 10;");
		out.append(newLine);
		out.append("\t\tr.e = -1;");
		out.append(newLine);
		out.append("\t} else if (a &lt; 10000) { //Our model supports up to 100 levels, so this should be the most it makes sense to check");
		out.append(newLine);
		out.append("\t\tr.b = a;");
		out.append(newLine);
		out.append("\t\tr.e = 0;");
		out.append(newLine);
		out.append("\t}");
		out.append(newLine);
		out.append("\treturn r;");
		out.append(newLine);
		out.append("}");
		out.append(newLine);
		
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
		out.append(newLine);
		
		out.append("timeActivity " + r.getId() + ";");
		out.append(newLine);
		out.append("const double_t k_" + r.getId() + " = " + formatDouble(r.get(Model.Properties.SCENARIO_PARAMETER_K).as(Double.class) / (m.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class) * r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class))) + ";");
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
			
			for (Reactant r : m.getSortedReactantList()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				outString = new StringWriter();
				Vector<Reaction> influencingReactions = new Vector<Reaction>();
				for (Reaction re : m.getReactionCollection()) {
					if (re.get(OUTPUT_REACTANT).as(String.class).equals(r.getId()))  { //If the reactant is downstream of a reaction, count that reaction
						influencingReactions.add(re);
					}
				}
				
				if (influencingReactions.size() < 1) {
					r.let(HAS_INFLUENCING_REACTIONS).be(false);
					continue;
				}
				r.let(HAS_INFLUENCING_REACTIONS).be(true);
				
				int myIndex = r.get(REACTANT_INDEX).as(Integer.class);
				
				StringBuilder template = new StringBuilder("<template><name>Reactant_" + r.getId() + "</name><parameter>int&amp; R, const int MAX</parameter>");
				template.append("<declaration>int[-1, 1] delta = 0, oldDelta = 0;\ntime_t tL, tU, tHalf;\ndouble_t totalRate = INFINITE_TIME_DOUBLE, oldRate = INFINITE_TIME_DOUBLE;\n\n");
				template.append("\ndouble_t compute_rate() {\n");
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
				
				if (influencingReactions.size() == 1) { // Compute the ".T" values to give the "activity ratio" of the reactions in ANIMO
					Reaction re = influencingReactions.get(0);
					if (uncertainty != 0) {
						template.append("\t" + re.getId() + ".T = round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
					} else {
						template.append("\t" + re.getId() + ".T = round(inverse(" + re.getId() + "_r));\n");
					}
				} else {
					for (Reaction re : influencingReactions) {
						if (uncertainty != 0) {
							template.append("\t" + re.getId() + ".T = " + "round(multiply(inverse(" + re.getId() + "_r), LOWER_UNC));\n");
						} else {
							template.append("\t" + re.getId() + ".T = " + "round(inverse(" + re.getId() + "_r));\n");
						}
					}
				}
				
				template.append("\treturn ");
				if (influencingReactions.size() == 1) {
					Reaction re = influencingReactions.get(0);
					int increment = re.get(INCREMENT).as(Integer.class);
					String subtr1 = (increment > 0)?"":"subtract(zero, ",
						   subtr2 = (increment > 0)?"":")";
					template.append(subtr1 + re.getId() + "_r" + subtr2 + ";\n");
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
				}
				
				template.append("}\n\nvoid update() {\n");
				
				//Finally compute the total rate
				template.append("\toldRate = totalRate;\n\ttotalRate = compute_rate();\n");
				
				
				template.append("\toldDelta = delta;\n");
				template.append("\tif (totalRate.b &lt; 0) {\n\t\tdelta = -1;\n\t\ttotalRate.b = -totalRate.b;\n\t} else {\n\t\tdelta = 1;\n\t}\n\tif (totalRate.b != 0) {\n");
				if (uncertainty != 0) {
					template.append("\t\ttL = round(multiply(inverse(totalRate), LOWER_UNC));\n\t\ttU = round(multiply(inverse(totalRate), UPPER_UNC));\n");
				} else {
					template.append("\t\ttL = round(inverse(totalRate));\n\t\ttU = tL;\n");
				}
				template.append("\t} else {\n\t\ttL = INFINITE_TIME;\n\t\ttU = INFINITE_TIME;\n\t}\n");
				template.append("\ttHalf = round(inverse(multiply(totalRate, TWO)));\n");
				template.append("\tif (tL != INFINITE_TIME &amp;&amp; tL &gt; tU) { //We use rounded things: maybe the difference between tL and tU was not so large, and with some rounding problems we could have this case\n\t\ttL = tU;\n\t}\n}\n\n");
				template.append("void react() {\n\tif (0 &lt;= R + delta &amp;&amp; R + delta &lt;= MAX) {\n\t\tR = R + delta;\n\t}\n\tupdate();\n}\n\n");
				template.append("bool rate_significantly_changed(double_t oldRate, double_t newRate) { //True if the new rate is at least double the old one, or if the sign of the rates is different (the reaction changes direction)\n\tdouble_t diff = subtract(multiply(newRate, inverse(oldRate)), TWO);\n\tif ((oldRate.b &lt; 0 &amp;&amp; newRate.b &gt;= 0 || oldRate.b &gt;= 0 &amp;&amp; newRate.b &lt; 0) || diff.b &gt;= 0) {\n\t\treturn true;\n\t}\n\treturn false;\n}\n\n");
				template.append("void decide_reset() {\n\tif (rate_significantly_changed(oldRate, totalRate)) { //If the updated conditions have significantly changed the rate, restart the reaction from the beginning, without considering the work already done.\n\t\tc[" + myIndex + "] = 0;\n\t}\n}\n\n");
				template.append("void decide_react() {\n\tif (rate_significantly_changed(totalRate, compute_rate())) { //If the update in conditions would significantly change the rate, rush the reaction (we have completed at least 50% of it: see the guard c[" + myIndex + "] &gt;= tHalf)\n\t\ttL = 0;\n\t\ttU = 0;\n\t\tc[" + myIndex + "] = 0;\n\t}\n}\n\n");
				template.append("bool can_react() {\n\treturn (tL != INFINITE_TIME &amp;&amp; ((delta &gt;= 0 &amp;&amp; R &lt; MAX) || (delta &lt; 0 &amp;&amp; R &gt; 0)));\n}\n\n");
				template.append("bool cant_react() {\n\treturn (tL == INFINITE_TIME || (delta &gt;= 0 &amp;&amp; R == MAX) || (delta &lt; 0 &amp;&amp; R == 0));\n}</declaration>");
				//template.append("bool someone_is_reacting() .... 
				template.append("<location id=\"id0\" x=\"-1896\" y=\"-728\"><name x=\"-1960\" y=\"-752\">stubborn</name><committed/></location>");
				template.append("<location id=\"id1\" x=\"-1528\" y=\"-728\"><committed/></location><location id=\"id6\" x=\"-1256\" y=\"-728\"><name x=\"-1248\" y=\"-752\">start</name><committed/></location>");
				template.append("<location id=\"id7\" x=\"-1552\" y=\"-856\"><name x=\"-1656\" y=\"-872\">not_reacting</name>" + (useOldResetting?"":"<label kind=\"invariant\" x=\"-1656\" y=\"-856\">c'==0</label>") + "</location>");
				template.append("<location id=\"id8\" x=\"-1416\" y=\"-728\"><name x=\"-1400\" y=\"-752\">updating</name><committed/></location>");
				template.append("<location id=\"id9\" x=\"-1664\" y=\"-728\"><name x=\"-1728\" y=\"-744\">waiting</name><label kind=\"invariant\" x=\"-1728\" y=\"-716\">c[" + myIndex + "] &lt;= tU</label></location>");
				template.append("<init ref=\"id6\"/>");
				template.append("<transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-744\">c[" + myIndex + "] &lt;= tU</label></transition>");
				template.append("<transition><source ref=\"id1\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1608\" y=\"-704\">c[" + myIndex + "] &gt; tU</label><label kind=\"assignment\" x=\"-1608\" y=\"-688\">c[" + myIndex + "] := tU</label><nail x=\"-1528\" y=\"-688\"/><nail x=\"-1608\" y=\"-688\"/></transition>");
				template.append("<transition><source ref=\"id0\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1816\" y=\"-632\">c[" + myIndex + "] &lt; tHalf</label><label kind=\"assignment\" x=\"-1832\" y=\"-616\">update(), decide_reset()</label><nail x=\"-1848\" y=\"-616\"/><nail x=\"-1464\" y=\"-616\"/></transition>");
				template.append("<transition><source ref=\"id0\"/><target ref=\"id9\"/><label kind=\"guard\" x=\"-1820\" y=\"-680\">c[" + myIndex + "] &gt;= tHalf</label><label kind=\"assignment\" x=\"-1832\" y=\"-664\">decide_react()</label><nail x=\"-1840\" y=\"-664\"/><nail x=\"-1744\" y=\"-664\"/></transition>");
				template.append("<transition><source ref=\"id6\"/><target ref=\"id8\"/><label kind=\"assignment\" x=\"-1344\" y=\"-728\">update()</label></transition>");
				template.append("<transition><source ref=\"id9\"/><target ref=\"id9\"/><label kind=\"select\" x=\"-1728\" y=\"-816\">i : int[0, N_REACTANTS-1]</label><label kind=\"guard\" x=\"-1728\" y=\"-816\">c[" + myIndex + "] &gt;= tL</label><label kind=\"synchronisation\" x=\"-1728\" y=\"-800\">reacting[i]?</label><label kind=\"assignment\" x=\"-1728\" y=\"-784\">react(), c[" + myIndex + "] := 0</label><nail x=\"-1738\" y=\"-800\"/><nail x=\"-1718\" y=\"-800\"/></transition>");
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
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c[" + myIndex + "] := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						case 1:
						case 2: //In this case, CATALYST = E1, REACTANT = E2 (the two upstream reactants)
							if (catalyst.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(catalyst)) {
								alreadyOutputReactants.add(catalyst);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(CATALYST).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c[" + myIndex + "] := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							if (reactant.get(REACTANT_INDEX).as(Integer.class) != r.get(REACTANT_INDEX).as(Integer.class) && !alreadyOutputReactants.contains(reactant)) {
								alreadyOutputReactants.add(reactant);
								template.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"synchronisation\" x=\"-1512\" y=\"" + y1 + "\">reacting[" + m.getReactant(re.get(REACTANT).as(String.class)).get(REACTANT_INDEX).as(Integer.class) + "]?</label><label kind=\"assignment\" x=\"-1528\" y=\"" + y2 + "\">update()" + (useOldResetting?", c[" + myIndex + "] := 0":"") + "</label><nail x=\"-1552\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y2 + "\"/><nail x=\"-1376\" y=\"" + y3 + "\"/></transition>");
								y1 += incrY;
								y2 += incrY;
							}
							break;
						default:
							break;
					}
				}
				template.append("<transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"guard\" x=\"-1512\" y=\"-840\">cant_react()</label><nail x=\"-1416\" y=\"-824\"/><nail x=\"-1552\" y=\"-824\"/></transition><transition><source ref=\"id8\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1512\" y=\"-744\">can_react()</label></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1576\" y=\"-816\">c[" + myIndex + "] &gt;= tL</label><label kind=\"synchronisation\" x=\"-1584\" y=\"-800\">reacting[" + myIndex + "]!</label><label kind=\"assignment\" x=\"-1592\" y=\"-784\">react(), c[" + myIndex + "] := 0</label><nail x=\"-1632\" y=\"-784\"/><nail x=\"-1464\" y=\"-784\"/></transition>");
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
				document = documentBuilder.parse(new ByteArrayInputStream(template.toString().getBytes()));
				tra.transform(new DOMSource(document), new StreamResult(outString));
				out.append(outString.toString());
				out.append(newLine);
				out.append(newLine);
			}
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
		out.append("int " + r.getId() + " := " + r.get(INITIAL_LEVEL).as(Integer.class) + ";");
		out.append(newLine);
		out.append("const int " + r.getId() + "Levels := " + r.get(NUMBER_OF_LEVELS).as(Integer.class) + ";");
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
