/*
 * Copyright 2011 by EDINA, University of Edinburgh, Landcare Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tikouka.nl.wps.algorithm;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.PlainStringBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.SAXException;
import tikouka.nl.wps.algorithm.util.Table2;
import tikouka.nl.wps.handler.XMLHandler2;
/**
 *
 * @author niels
 */
public class SoilScalingAlgorithm extends AbstractObservableAlgorithm
{

    private List<String> errors = new ArrayList<String>();


    public SoilScalingAlgorithm(){
        super();
    }

     public Class getOutputDataType(String id) {
        return LiteralStringBinding.class;
    }
/*
     * @see org.n52.wps.server.IAlgorithm#getErrors()
     */
    public List<String> getErrors()
    {
        return errors;
    }

    public Class getInputDataType(String id) {
        if(id.equalsIgnoreCase("scalefactor")){
		return LiteralStringBinding.class;
        }
        else if(id.equalsIgnoreCase("fsl")){
		return GTVectorDataBinding.class;
        }
	throw new RuntimeException("Could not find datatype for id " + id);
    }

    /*
     * @see org.n52.wps.server.IAlgorithm#run(java.util.Map)
     */
    public Map<String, IData> run(Map<String, List<IData>> inputData) {
       // ############################################################
        // READ THE INPUT DATA
        // ############################################################

        if(inputData==null || !inputData.containsKey("fsl")){
			throw new RuntimeException("Error while allocating input parameters");
		}
		List<IData> dataList = inputData.get("fsl");
		if(dataList == null || dataList.size() != 1){
			throw new RuntimeException("Error while allocating input parameters");
		}
		IData fslData = dataList.get(0);

		FeatureCollection fslColl = ((GTVectorDataBinding) fslData).getPayload();

        if(inputData==null || !inputData.containsKey("scalefactor")){
			throw new RuntimeException("Error while allocating input parameters");
		}
         List<IData> scalefactor = inputData.get("scalefactor");


        // ############################################################
        //  PARSE THE LOOKUPTABLE
        // ############################################################
        List<Table2> scalefactorList = getLookupTableData(scalefactor);

        // ############################################################
        //  RUN THE MODEL
        // ############################################################

        //int totalNumberOfFeatures = fslColl.size();
//        for (Iterator ia = fslColl.iterator(); ia.hasNext(); ) {
//            SimpleFeature fa = (SimpleFeature) ia.next();
//            Object praw = fa.getAttribute("praw_class");
//            String val = (String)praw;
//        }
        SimpleFeature fsl = (SimpleFeature) fslColl.features().next();
        String praw = (String)fsl.getAttribute("PRAW_CLASS");
        String drain = (String)fsl.getAttribute("DRAIN_CLAS");
        String key = (String)drain+","+(String)praw;
//        int prawint = fsl.getAttribute("PRAW_CLASS").hashCode();
//        int drainint = fsl.getAttribute("DRAIN_CLAS").hashCode();
        String val = null;
        for (int k=0;k<scalefactorList.size();k++){
            if (scalefactorList.get(k).getKey().equalsIgnoreCase(key)){
                val = scalefactorList.get(k).getValue();
                break;
            };
        }
        if (val == null){
            val = "No match found";
        }
        // ############################################################
        //  WRITE THE OUTPUT DATA
        // ############################################################
        HashMap<String,IData> resulthash = new HashMap<String,IData>();
        try{

            resulthash.put("result", new PlainStringBinding(val));
         }catch(Exception e){
            e.printStackTrace();
        }
        return resulthash;
    }

     private List<Table2> getLookupTableData(List<IData> lookup)
	{
                XMLHandler2 handler = new XMLHandler2();
		try
		{
                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

                    parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
                    parser.parse(new ByteArrayInputStream(((LiteralStringBinding) lookup.get(0)).getPayload().getBytes()), handler);

                }catch(SAXException se){
                    se.printStackTrace();
                    throw new RuntimeException(se);
                }catch(ParserConfigurationException pe){
                    pe.printStackTrace();
                    throw new RuntimeException(pe);
                }catch(IOException ioe){
                    ioe.printStackTrace();
                    throw new RuntimeException(ioe);
                }

                return handler.getRasterRable().getTable();

	}

         static protected GenericFileData getURL(String u) {
             GenericFileData gmlData = null;
             //String gml = null;
             URL url;
             InputStream is;
             InputStreamReader isr;
             BufferedReader r;
             String str;

            try {
              //System.out.println("Reading URL: " + u);
              url = new URL(u);
              is = url.openStream();
              gmlData = new GenericFileData(is, null);
//              isr = new InputStreamReader(is);
//              r = new BufferedReader(isr);
//              do {
//                str = r.readLine();
//                if (str != null)
//                  //System.out.println(str);
//
//                    gmlData = gmlData + str;
//              } while (str != null);
            } catch (MalformedURLException e) {
              System.out.println("Must enter a valid URL");
            } catch (IOException e) {
              System.out.println("Can not connect");
            }

             return gmlData;
      }
}