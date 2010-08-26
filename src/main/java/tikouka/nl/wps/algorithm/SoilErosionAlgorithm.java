/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tikouka.nl.wps.algorithm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.AbstractProcessor;
import org.geotools.coverage.processing.DefaultProcessor;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTRasterDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import tikouka.nl.wps.algorithm.util.lookuptable;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 *
 * @author niels
 */
public class SoilErosionAlgorithm extends AbstractObservableAlgorithm
{

    private List<String> errors = new ArrayList<String>();
    final AbstractProcessor processor = new DefaultProcessor(null);

    public SoilErosionAlgorithm()
    {
        super();
    }

    public Class getOutputDataType(String id) {
        return GTRasterDataBinding.class;
    }
/*
     * @see org.n52.wps.server.IAlgorithm#getErrors()
     */
    public List<String> getErrors()
    {
        return errors;
    }

    public Class getInputDataType(String id) {
        if(id.equalsIgnoreCase("nz_woody")||id.equalsIgnoreCase("nz_ak2")||id.equalsIgnoreCase("nz_r2")){
				return GTRasterDataBinding.class;
        }
        else if (id.equalsIgnoreCase("nz_woody_lookup")){
            return LiteralStringBinding.class;
        }
        else if (id.equalsIgnoreCase("rain_factor")){
            return LiteralDoubleBinding.class;
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
        if(inputData==null || !inputData.containsKey("nz_woody_lookup")){
			throw new RuntimeException("Error while allocating input parameters");
		}
        List<IData> nz_woody_lookup = inputData.get("nz_woody_lookup");

        if(inputData==null || !inputData.containsKey("rain_factor")){
			throw new RuntimeException("Error while allocating input parameters");
		}
        double rain_factor[] = new double[1];
        rain_factor[0] = ((LiteralDoubleBinding) inputData.get("rain_factor").get(0)).getPayload();

        if(inputData==null || !inputData.containsKey("nz_woody")){
			throw new RuntimeException("Error while allocating input parameters 'landuse'");
		}
        GridCoverage2D nz_woody = ((GTRasterDataBinding) inputData.get("nz_woody").get(0)).getPayload();

        if(inputData==null || !inputData.containsKey("nz_ak2")){
			throw new RuntimeException("Error while allocating input parameters 'landuse'");
		}
        GridCoverage2D nz_ak2 = ((GTRasterDataBinding) inputData.get("nz_ak2").get(0)).getPayload();

        if(inputData==null || !inputData.containsKey("nz_r2")){
			throw new RuntimeException("Error while allocating input parameters 'landuse'");
		}
        GridCoverage2D nz_r2 = ((GTRasterDataBinding) inputData.get("nz_r2").get(0)).getPayload();
         // ############################################################
        //  PARSE THE LOOKUPTABLE
        // ############################################################

        List<lookuptable> woodylutList = new ArrayList<lookuptable>();
        getLookupTableData(nz_woody_lookup, woodylutList);

        // ############################################################
        //  RUN THE MODEL
        // ############################################################

        Envelope2D res_env = new Envelope2D(nz_woody.getEnvelope2D());
                Rectangle2D.intersect(nz_woody.getEnvelope2D(), nz_ak2.getEnvelope2D(), res_env);

        Rectangle2D.intersect(res_env, nz_r2.getEnvelope2D(), res_env);

        res_env.setRect((int)res_env.x, (int)res_env.y, (int)res_env.width, (int)res_env.height);

        ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        //ParameterValueGroup param = processor.getOperation("crop").getParameters();
        param.parameter("Source").setValue(nz_woody);
        param.parameter("envelope").setValue(res_env);
        GridCoverage2D nz_woody_cr = (GridCoverage2D) processor.doOperation(param);

        param.parameter("Source").setValue(nz_ak2);
        param.parameter("envelope").setValue(res_env);
        GridCoverage2D nz_ak2_cr = (GridCoverage2D) processor.doOperation(param);

        param.parameter("Source").setValue(nz_r2);
        param.parameter("envelope").setValue(res_env);
        GridCoverage2D nz_r2_cr = (GridCoverage2D) processor.doOperation(param);

        //first step is to multiply the r2 coverage with the rain factor
        //ParameterValueGroup param2 = processor.getOperation("multiply").getParameters();
        ParameterValueGroup param2 = processor.getOperation("MultiplyConst").getParameters();
        param2.parameter("Source").setValue(nz_r2_cr);
        param2.parameter("constants").setValue(rain_factor);
        GridCoverage2D nz_r2_cr2 = (GridCoverage2D) processor.doOperation(param2);

        int minX = (int)res_env.getMinX();
        int maxX = (int)res_env.getMaxX();
        int width = (int)res_env.getWidth()/100;
        int x_x =  (int)((maxX - minX)/(width));
        int i = 0;

        int minY = (int)res_env.getMinY();
        int maxY = (int)res_env.getMaxY();
        int height = (int)res_env.getHeight()/100;
        int y_y =  (int)((maxY - minY)/(height));
        int j = 0;

        //BufferedImage image = new BufferedImage((int)width,(int)height, BufferedImage.TYPE_BYTE_GRAY);
        //BufferedImage image = new BufferedImage((int)width,(int)height, BufferedImage.TYPE_3BYTE_BGR);
        //WritableRaster raster = image.getRaster();
        float[][] raster = new float[height][width];
        
        /* Algorithm:
         * a is the geology & slope information
         * C is the land use, either woody/forest or other bare ground/grass
         * D is the connectivity to water/streams(a constant of 1 in the case of New Zealand)
         * R is the mean annual rainfall squared
         * SE is the resulting soil erosion
         *
         * a * grow(C,woody/forest, g) * D * r^2 * R = SE, with g and r provided by the student.
         *
         *  The grow function can be thought of as similar to buffer, but it acts on a single category
         * (ie woody/forest) within a raster and ?grows? the extent of that category by a fixed width that
         * I have specified as g in the formula, which would be provided by the student.
         * g values can be +ve or -ve, which would indicate a contraction in forest area.
         *
         * I think SE is in t/km^2/yr/mm^2
         */

        //GeometryFactory geomFac = new GeometryFactory();

        try{
            for (int y=minY + (y_y/2) ;y < maxY;y+= y_y){
                i=0;
                for (int x = minX + (x_x/2) ;x < maxX;x+= x_x){
                    int[] woodyval= new int[1];
                    double[] r2_rval = new double[1];
                    double[] ak2_rval = new double[1];
                    double[] out = new double[1];


                    Point2D pt = new DirectPosition2D(x, y);
                    
                    nz_woody_cr.evaluate(pt, woodyval);
                    //System.out.println("woodyval: "+ woodyval[0]);
                    nz_r2_cr2.evaluate(pt, r2_rval);
                    //System.out.println("r2: "+ r2_rval[0]);
                    nz_ak2_cr.evaluate(pt, ak2_rval);
                    //System.out.println("ak2: "+ ak2_rval[0]);

                    double woodyvallookup = woodylutList.get(woodyval[0]).getValue();
                    //System.out.println(luval[0]);
                    //out[0] = woodyvallookup * r2_rval[0] * ak2_rval[0];
                    out[0] = woodyvallookup * r2_rval[0] * ak2_rval[0];

                    raster[j][i]= (float)out[0];


                    //out[0] = woodyvallookup * r2_rval[0] * ak2_rval[0];
                    System.out.println("raster y: " + String.valueOf(j));
                    System.out.println("raster x: " + String.valueOf(i));
                    System.out.println("raster out: " + raster[j][i]);
                   //raster.setPixel( (x - minX) / x_x, (y - minY) / y_y,out);
                   i++;
                }
                j++;
            }
        }catch(NullPointerException npe){
            npe.printStackTrace();
        }catch(ArrayIndexOutOfBoundsException aie){
            aie.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }

        //for some reason the image is flipped vertically.
        //Use a transformation to flip it back before writing the coverage
//        AffineTransform at = AffineTransform.getScaleInstance(1, -1);
//        at.translate(0, -image.getHeight(null));
//        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//        raster = op.filter(raster,null);

        GridCoverageFactory coverageFactory = new GridCoverageFactory();
//        double[] testres = new double[1];
//        raster.getPixel(0, 0, testres);
//        System.out.println(testres[0]);

        //MathTransform mt = landuse_cr.getGridGeometry().getGridToCRS();
        //GridSampleDimension[] gsd = new GridSampleDimension[raster.getNumBands()];
        //CoordinateReferenceSystem crs = landuse.getCoordinateReferenceSystem();

        // ############################################################
        //  WRITE THE OUTPUT DATA
        // ############################################################
        HashMap<String,IData> resulthash = new HashMap<String,IData>();
        try{
            //TODO: java.lang.IllegalArgumentException: Illegal transform of type "LinearTransform1D"
            GridCoverage2D coverage = coverageFactory.create("output", raster,res_env);

            resulthash.put("result", new GTRasterDataBinding(coverage));
         }catch(Exception e){
            e.printStackTrace();
        }
        return resulthash;
    }

    private void getLookupTableData(List<IData> lookup, List<lookuptable> list)
	{
		try
		{
			SAXReader reader = new SAXReader();
			Document document = reader.read(new ByteArrayInputStream(((LiteralStringBinding) lookup.get(0)).getPayload().getBytes()));

			List<Element> xmlRasterTableList = (List<Element>) document.getRootElement().elements();

			for (Element xmlRasterTable : xmlRasterTableList)
			{
				String RasterTableName = xmlRasterTable.attributeValue("id");

				List<Element> xmlTableList = (List<Element>) xmlRasterTable.elements();

				for (Element xmlTable : xmlTableList)
				{

					String id = xmlTable.attributeValue("id");
					String key = xmlTable.attributeValue("key");
                                        String value = xmlTable.attributeValue("value");

					lookuptable temp = new lookuptable(RasterTableName + ":" + id, key, Integer.parseInt(value));

					list.add(temp);
				}
			}

		}
		catch (DocumentException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
