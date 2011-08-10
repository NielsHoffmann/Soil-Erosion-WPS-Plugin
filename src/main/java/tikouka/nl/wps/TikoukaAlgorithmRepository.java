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

package tikouka.nl.wps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.opengis.wps.x100.ProcessDescriptionType;

import org.apache.log4j.Logger;
import org.n52.wps.PropertyDocument.Property;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.ITransactionalAlgorithmRepository;
import org.n52.wps.server.request.ExecuteRequest;
/**
 * A static repository to retrieve the available algorithms.
 * @author foerster
 * 05/08/2010 NH copied from LocalAlgorithmRepository to start Tikouka Repository
 *
 */
public class TikoukaAlgorithmRepository implements ITransactionalAlgorithmRepository{

    private static Logger LOGGER = Logger.getLogger(TikoukaAlgorithmRepository.class);
	private Map<String, String> algorithmMap;
	private Map<String, ProcessDescriptionType> processDescriptionMap;

	public TikoukaAlgorithmRepository() {
		algorithmMap = new HashMap<String, String>();
		processDescriptionMap = new HashMap<String, ProcessDescriptionType>();

                // check if the repository is active
		if(WPSConfig.getInstance().isRepositoryActive(this.getClass().getCanonicalName())){
			Property[] propertyArray = WPSConfig.getInstance().getPropertiesForRepositoryClass(this.getClass().getCanonicalName());
			for(Property property : propertyArray){
				// check the name and active state
				if(property.getName().equalsIgnoreCase("Algorithm") && property.getActive()){
					addAlgorithm(property.getStringValue());
				}
			}
		} else {
			LOGGER.debug("Tikouka Algorithm Repository is inactive.");
		}

	}

	public boolean addAlgorithms(String[] algorithms)  {
		for(String algorithmClassName : algorithms) {
			addAlgorithm(algorithmClassName);
		}
		LOGGER.info("Algorithms registered!");
		return true;

	}

	public IAlgorithm getAlgorithm(String className, ExecuteRequest executeRequest) {
		try {
			return loadAlgorithm(algorithmMap.get(className));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Collection<IAlgorithm> getAlgorithms() {
		Collection<IAlgorithm> resultList = new ArrayList<IAlgorithm>();
		try {
			for(String algorithmClasses : algorithmMap.values()){
				resultList.add(loadAlgorithm(algorithmMap.get(algorithmClasses)));
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return resultList;
	}

	public Collection<String> getAlgorithmNames() {
		return new ArrayList<String>(algorithmMap.keySet());
	}

	public boolean containsAlgorithm(String className) {
		return algorithmMap.containsKey(className);
	}

	private IAlgorithm loadAlgorithm(String algorithmClassName) throws Exception{
		IAlgorithm algorithm = (IAlgorithm)TikoukaAlgorithmRepository.class.getClassLoader().loadClass(algorithmClassName).newInstance();
		if(!algorithm.processDescriptionIsValid()) {
			LOGGER.warn("Algorithm description is not valid: " + algorithmClassName);
			throw new Exception("Could not load algorithm " +algorithmClassName +". ProcessDescription Not Valid.");
		}
		return algorithm;
	}

	public boolean addAlgorithm(Object processID) {
		if(!(processID instanceof String)){
			return false;
		}
		String algorithmClassName = (String) processID;

		algorithmMap.put(algorithmClassName, algorithmClassName);
		LOGGER.info("Algorithm class registered: " + algorithmClassName);



		return true;

	}

	public boolean removeAlgorithm(Object processID) {
		if(!(processID instanceof String)){
			return false;
		}
		String className = (String) processID;
		if(algorithmMap.containsKey(className)){
			algorithmMap.remove(className);
			return true;
		}
		return false;
	}

	@Override
	public ProcessDescriptionType getProcessDescription(String processID) {
		if(!processDescriptionMap.containsKey(processID)){
			processDescriptionMap.put(processID, getAlgorithm(processID, null).getDescription());
		}
		return processDescriptionMap.get(processID);
	}
}
