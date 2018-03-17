package cs6378.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class NodeUtil {

	public static void main(String[] args) {
		HashMap<Integer, String> res = new HashMap<>();
		HashMap<Integer, Integer> pos = new HashMap<>();
		NodeInfo nodeInfo = readConfig("config2.txt", res, pos );
		System.out.println(res);
		System.out.println(pos);
		System.out.println(nodeInfo);
	}

	/**
	 * read node info from config file and initialize client instance according to
	 * config format
	 * 
	 * @param fileName
	 *            given config file name
	 * @return return a list of client info
	 * @throws FileNotFoundException
	 */
	public static NodeInfo readConfig(String fileName, Map<Integer, String> id_to_addr, Map<Integer, Integer> id_to_index) {
		Scanner scanner = null;
		Map<Integer, String> nodeInfos = new HashMap<>();
		NodeInfo nodeInfo = new NodeInfo();
		try {
			scanner = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (scanner != null) {
			// read number of nodes
			int nodeNums = 0;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (!line.startsWith("#") && line.length() > 0) {
					nodeNums = Integer.parseInt(line.trim());
					break;
				}
			}

			int index = 0;
			while (index < nodeNums && scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.length() == 0) {
					continue;
				}
				if (!line.startsWith("#")) {
					String[] info = line.split("\\s+");
					id_to_addr.put(Integer.parseInt(info[0]), info[2] + ":" + info[1]);
					id_to_index.put(Integer.parseInt(info[0]), index);
					index++;
				}
			}
			System.out.println("Reading Neighbors");
			
			index = 0;
			
			while (index < nodeNums && scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.length() == 0) {
					continue;
				}
				if (!line.startsWith("#")) {
					String[] info = line.split("\\s+");
					nodeInfos.put(Integer.parseInt(info[0]), info[1]);
					index++;
				}
			}
			
			System.out.println("Reading Config Ends");
			scanner.close();
		}
		nodeInfo.setNodeInfos(nodeInfos);
		return nodeInfo;
	}
}
