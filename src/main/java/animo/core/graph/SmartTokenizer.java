package animo.core.graph;

/*
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA.
 */


import java.util.ArrayList;


/**
 * A StringTokenizer class that handle empty tokens.
 * 
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */
public class SmartTokenizer
{
    /**
     * Testing this class.
     * 
     * @param args  Not used.
     */
    public static void main(String[] args)
    {
        SmartTokenizer t = new SmartTokenizer("This,is,a,,test,", ",");
        while (t.hasMoreTokens())
        {
            String token = t.nextToken();
            System.out.println("#" + token + "#");
        }
    }

    private ArrayList<String> tokens;


    private int current;


    public SmartTokenizer(String string, String delimiter)
    {
        tokens = new ArrayList<String>();
        current = 0;

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(string, delimiter, true);

        boolean wasDelimiter = true;
        boolean isDelimiter = false;

        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();

            isDelimiter = token.equals(delimiter);

            if (wasDelimiter)
                tokens.add(isDelimiter ? "" : token);
            else if (!isDelimiter)
                tokens.add(token);

            wasDelimiter = isDelimiter;
        }

        if (isDelimiter)
            tokens.add("");
    }


    public int countTokens()
    {
        return tokens.size();
    }


    public boolean hasMoreElements()
    {
        return hasMoreTokens();
    }


    public boolean hasMoreTokens()
    {
        return current < tokens.size();
    }


    public Object nextElement()
    {
        return nextToken();
    }


    public String nextToken()
    {
        if (current >= tokens.size())
            return null; //This way it is even "smarter"
        String token = tokens.get(current);
        current++;
        return token;
    }
}
