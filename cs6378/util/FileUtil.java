package cs6378.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class FileUtil {
	public static final String FILE_ADDR = "fileinfo.txt";
	public static HashMap<String, List<String>> readFileConfig() {
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
			while(index < num && scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if(line.length() > 0 && line.matches("^.+\\.txt$")) {
					String[] info = line.split("\\s+");
					List<String> list = new ArrayList<>();
					String[] filenames = info[1].split(",");
					for(String filename : filenames) {
						list.add(filename.trim());
					}
					file_to_chunks.put(info[0], list);
					index++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return file_to_chunks;
	}
	
	public static void main(String[] args) {
		System.out.println(FileUtil.readFileConfig());
	}

}
