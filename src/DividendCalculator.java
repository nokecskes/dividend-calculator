import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Map.Entry;

public class DividendCalculator {
	
	public static void main(String[] args) {
		if (args.length > 0 && args[0].length() > 0) {
			String fileName = args[0];
			List<List<String>> records = readRecords(fileName);
			
			Map<String, Map<String, BigDecimal>> dividendsBySalesRepresentatives = calculateDividend(records);
			
			exportResult(dividendsBySalesRepresentatives);
		} else { 
			System.out.println("No file received.");
		}
	}

	private static Map<String, Map<String, BigDecimal>> calculateDividend(List<List<String>> records) {
		Map<String, Map<String, BigDecimal>> soldProductsBySalesRepresentatives = sumRecords(records);
		Map<String, Map<String, BigDecimal>> dividendsBySalesRepresentatives = calculateDividentAndBonus(soldProductsBySalesRepresentatives);
		return dividendsBySalesRepresentatives;
	}

	private static List<List<String>> readRecords(String fileName) {
		List<List<String>> records = new ArrayList<>();
		try {
			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			        String[] values = line.split("\\|");
			        records.add(Arrays.asList(values));
			    }
			}
		} catch (IOException e) {
			System.out.println("Could not parse input file. Exception message: " + e.getMessage());
		}
		return records;
	}
	
	private static Map<String, Map<String, BigDecimal>> sumRecords(List<List<String>> records) {
		Map<String, Map<String, BigDecimal>> soldProductsBySalesRepresentatives = new HashMap<>();
		
		for (List<String> record : records) {
			if (record.size() != 3) {
				logInvalidDataError(record);
				continue;
			} 
			
			// Record example: A|Ü1|800000
			String product = record.get(0);
			String salesRepresentative = record.get(1);
			BigDecimal sold = new BigDecimal(record.get(2));
			
			Map<String, BigDecimal> soldSumByProducts = new HashMap<>();
			if (soldProductsBySalesRepresentatives.containsKey(salesRepresentative)) {
				soldSumByProducts = soldProductsBySalesRepresentatives.get(salesRepresentative);
			}
			
			if (soldSumByProducts.containsKey(product)) {
				sold = sold.add(soldSumByProducts.get(product));
			}
			
			soldSumByProducts.put(product, sold);
			soldProductsBySalesRepresentatives.put(salesRepresentative, soldSumByProducts);
		}
		
		return soldProductsBySalesRepresentatives;
	}
	
	private static Map<String, Map<String, BigDecimal>> calculateDividentAndBonus(
		Map<String, Map<String, BigDecimal>> soldProductsBySalesRepresentatives) {
		
		SortedMap<String, SortedMap<BigDecimal, BigDecimal>> bonusesByProducts = getBonusesByProducts();
		
		Map<String, Map<String, BigDecimal>> dividendsBySalesRepresentatives = new HashMap<>();
		for (Map.Entry<String,Map<String, BigDecimal>> salesRepresentativeEntry : soldProductsBySalesRepresentatives.entrySet()) {
			
			for(Map.Entry<String, BigDecimal> soldProductEntry : salesRepresentativeEntry.getValue().entrySet()) {
				String salesRepresentative = salesRepresentativeEntry.getKey();
				String product = soldProductEntry.getKey();
				BigDecimal sold = soldProductEntry.getValue();
				
				BigDecimal bonusDividend = calculateBonusDivident(soldProductEntry, bonusesByProducts);
				BigDecimal dividend = sold.multiply(new BigDecimal(0.01));
				dividend = dividend.add(bonusDividend);
				
				Map<String, BigDecimal> dividends = new HashMap<>();
				if (dividendsBySalesRepresentatives.containsKey(salesRepresentative)) {
					dividends = dividendsBySalesRepresentatives.get(salesRepresentative);
				}
				dividends.put(product, dividend);
				dividendsBySalesRepresentatives.put(salesRepresentative, dividends);
			}
		}
		
		return dividendsBySalesRepresentatives;
			
	}
	
	private static void logInvalidDataError(List<String> record)  {
		StringBuilder sb = new StringBuilder("Invalid record line, with invalid data number. Invalid line: ");
		for (String recordElement : record) {
			sb.append(recordElement);
		}
		String logMessage = sb.toString();
		System.out.println(logMessage);
	}
	
	private static BigDecimal calculateBonusDivident(Entry<String, BigDecimal> soldProductEntry, SortedMap<String, SortedMap<BigDecimal, BigDecimal>> bonusesByProducts) {
		String product = soldProductEntry.getKey();
		BigDecimal sold = soldProductEntry.getValue();
		
		BigDecimal bonus = BigDecimal.ZERO;
		SortedMap<BigDecimal, BigDecimal> productBonuses = bonusesByProducts.get(product);
		for (Map.Entry<BigDecimal, BigDecimal> productBonus : productBonuses.entrySet()) {
			if (sold.compareTo(productBonus.getKey()) > -1) {
				bonus = productBonus.getValue();
			}
		}
		
		return bonus;
	}
	
	private static SortedMap<String, SortedMap<BigDecimal, BigDecimal>> getBonusesByProducts() {
		SortedMap<String, SortedMap<BigDecimal, BigDecimal>> bonusesByProducts = new TreeMap<>();
		
		SortedMap<BigDecimal, BigDecimal> bonusesProductA = new TreeMap<>();
		bonusesProductA.put(new BigDecimal(20000000), new BigDecimal(40000));
		bonusesProductA.put(new BigDecimal(10000000), new BigDecimal(25000));
		bonusesByProducts.put("A", bonusesProductA);
		
		SortedMap<BigDecimal, BigDecimal> bonusesProductB = new TreeMap<>();
		bonusesProductB.put(new BigDecimal(16000000), new BigDecimal(50000));
		bonusesProductB.put(new BigDecimal(8000000), new BigDecimal(30000));
		bonusesByProducts.put("B", bonusesProductB);
		
		SortedMap<BigDecimal, BigDecimal> bonusesProductC = new TreeMap<>();
		bonusesProductC.put(new BigDecimal(10000000), new BigDecimal(40000));
		bonusesProductC.put(new BigDecimal(5000000), new BigDecimal(20000));
		bonusesByProducts.put("C", bonusesProductC);
		
		return bonusesByProducts;
	}
	
	private static void exportResult(Map<String, Map<String, BigDecimal>> dividendsBySalesRepresentatives) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		    Document doc = docBuilder.newDocument();
		     
		    Element rootElement = doc.createElement("dividends");
		    doc.appendChild(rootElement);
		      
			for (Entry<String, Map<String, BigDecimal>> salesRepresentativeEntry : dividendsBySalesRepresentatives.entrySet()) {
				String salesRepresentative = salesRepresentativeEntry.getKey();
				Element salesRepresentativeE = doc.createElement("salesRepresentative");
				salesRepresentativeE.setTextContent(salesRepresentative);
				rootElement.appendChild(salesRepresentativeE);

				for(Entry<String, BigDecimal> soldProductEntry : salesRepresentativeEntry.getValue().entrySet()) {
					String product = soldProductEntry.getKey();
					Element productE = doc.createElement("product");
					productE.setTextContent(product);
					salesRepresentativeE.appendChild(productE);

					BigDecimal dividend =  soldProductEntry.getValue();
					Element dividendE = doc.createElement("dividend");
					dividendE.setTextContent(dividend.toString());
					salesRepresentativeE.appendChild(dividendE);
				}
			}
			
			write(doc);
		} catch (ParserConfigurationException| TransformerException e) {
			System.out.println("Could not export results. Exception message: " + e.getMessage());
		} 
	}

	private static void write(Document doc) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();

	    // pretty print
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	    DOMSource source = new DOMSource(doc);
	    StreamResult result = new StreamResult(new File("dividends.xml"));

	    transformer.transform(source, result);
	}
}
