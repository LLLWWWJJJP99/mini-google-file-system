package cs6378.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class FileUtil {
	public static final String FILE_ADDR = "fileinfo.txt";
	/**
	 * read the config file and store the information about how a file corresponds to its chunks
	 * @return hashmap about file to chunks info
	 */
	public static HashMap<String, List<String>> readFileConfig(Map<String, String[]> chunk_to_replicas) {
		HashMap<String, List<String>> file_to_chunks = new HashMap<>();
		try(Scanner scanner = new Scanner(new FileInputStream(FILE_ADDR))){
			int num = 0 ;
			while(scanner.hasNext()) {
				String str = scanner.nextLine().trim();
				if(str.length() == 0) {
					continue;
				}else {
					num = Integer.parseInt(str.trim());
					break;
				}
			}
			int index = 0;
			
			int chunk_count = 0;
			while(index < num && scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if(line.length() > 0 && line.matches("^.+\\.txt$")) {
					String[] info = line.split("\\s+");
					List<String> list = new ArrayList<>();
					String[] filenames = info[1].split(",");
					for(String filename : filenames) {
						chunk_count++;
						list.add(filename.trim());
					}
					file_to_chunks.put(info[0], list);
					index++;
				}
			}
			
			index = 0;
			while(index < chunk_count && scanner.hasNextLine()) {
				String str = scanner.nextLine().trim();
				if(str.length() == 0) {
					continue;
				}else {
					String[] replics_info = str.split("\\|");
					String[] replicas = replics_info[1].split(",");
					if(replicas.length != 3) {
						throw new RuntimeException("Config file wrongs");
					}
					chunk_to_replicas.putIfAbsent(replics_info[0], replicas);
					index++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return file_to_chunks;
	}
	
	public static void main(String[] args) {
		HashMap<String, String[]> record = new HashMap<>();
		System.out.println(FileUtil.readFileConfig(record));
		for(Map.Entry<String, String[]> entry : record.entrySet()) {
			String[] strs = entry.getValue();
			StringBuilder sb = new StringBuilder();
			for(String str : strs) {
				sb.append(str);
				sb.append(",");
			}
			System.out.println(entry.getKey() + " " + sb.toString());
		}
	}

}
