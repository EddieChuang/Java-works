package excelOperate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.*;

import jsonOperate.JSONOperator;

public class ReadWriteExcelFile {
	
	ReadWriteExcelFile(){
	}
	
	
	private static JSONObject iterateXSSFCells(Iterator<Cell> cells, ArrayList<String> headers, int i){
		
		if(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				try {
					JSONObject jsonObject = new JSONObject();
					JSONArray children = new JSONArray();
					String header = headers.get(i);
					boolean isLeaf = headers.size() == i+2;//.equals("名");
					System.out.println(headers.size() + ", " + (i+2));
					if(!isLeaf){
						children.put(iterateXSSFCells(cells, headers, i+1));
					} else {
						// 讀取 "註解"
						jsonObject.put("note", cells.hasNext() ? cells.next().getStringCellValue() : "");
					}
					
					jsonObject.put("children", children);
					jsonObject.put("leaf", isLeaf);
					jsonObject.put("text", cell.getStringCellValue());
					jsonObject.put("cat", header);
					return jsonObject;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		return new JSONObject();
	}
	
	private static JSONObject iterateXSSFCellsJstree(Iterator<Cell> cells, ArrayList<String> headers, int i){
		
		if(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				try {
					JSONObject jsonObject = new JSONObject();
					JSONArray children = new JSONArray();
					String header = headers.get(i);
					boolean isLeaf = header.equals("名");
					if(!isLeaf){
						children.put(iterateXSSFCellsJstree(cells, headers, i+1));
					} else {
						String note = cells.hasNext() ? cells.next().getStringCellValue() : "無";
						jsonObject.put("note", note.equals("") ? "無" : note);
					}
					
					jsonObject.put("cat", header);
					jsonObject.put("type", "subDir");
					jsonObject.put("leaf", isLeaf);
					jsonObject.put("children", children);
					jsonObject.put("text", cell.getStringCellValue());
					return jsonObject;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("deprecation")
	private static JSONObject iterateXSSFCellsJstreeNoLeaves(Iterator<Cell> cells, ArrayList<String> headers, int i){
		
		if(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				try {
					JSONObject jsonObject = new JSONObject();
					JSONArray children = new JSONArray();
					String header = headers.get(i);
					boolean isLeaf = headers.size() == i + 2;
					if(!isLeaf){
						JSONObject child = iterateXSSFCellsJstreeNoLeaves(cells, headers, i+1);
						if(child != null)
							children.put(child);
						jsonObject.put("cat", header);
						jsonObject.put("type", "subDir");
						jsonObject.put("children", children);
						jsonObject.put("text", cell.getStringCellValue());
						return jsonObject;
					}
					
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	// 忽略空字串
	public static ArrayList<String> XSSFRowToHeader(XSSFRow row){
		
		ArrayList<String> header = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		
		while(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				header.add(cell.getStringCellValue());
			}
		}
		return header;
	}
	
	// 保留空字串
	public static ArrayList<String> XSSFRowToArrayList(XSSFRow row){
		
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		
		while(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				list.add(cell.getStringCellValue());
			} else {
				list.add("");
			}
		}
		
		return list;
	}
	
	public static JSONObject XSSFRowToJSONObject(XSSFRow row, ArrayList<String> header){
		
		Iterator<Cell> cells = row.cellIterator();
		return iterateXSSFCells(cells, header, 0);
	}
	
	public static JSONObject XSSFRowToJstree(XSSFRow row, ArrayList<String> header, boolean hasLeaves){
		
		Iterator<Cell> cells = row.cellIterator();
		if(hasLeaves)
			return iterateXSSFCellsJstree(cells, header, 0);
		else
			return iterateXSSFCellsJstreeNoLeaves(cells, header, 0);
		
	}
	
	public static JSONObject XLSXToJSON(String path, String filename){
		
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		int count = 0;
		try {
			InputStream excelToRead = new FileInputStream(path + filename);			
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			ArrayList<String> headers = ReadWriteExcelFile.XSSFRowToHeader((XSSFRow) rows.next());
			while(rows.hasNext()){
				
				XSSFRow row = (XSSFRow) rows.next();
				JSONObject jsonRow = XSSFRowToJSONObject(row, headers);
				jsonArray = JSONOperator.append(jsonArray, jsonRow);
			}
			json.put("cat", "目錄");
			json.put("text", filename.split("\\.")[0]);
			json.put("children", jsonArray);
			workbook.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONObject XLSXToJstreeJSON(String path, String filename){
		
		System.out.println("z");
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		int count = 0;
		try {
			InputStream excelToRead = new FileInputStream(path + filename);
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			ArrayList<String> headers = ReadWriteExcelFile.XSSFRowToHeader((XSSFRow) rows.next());
			while(rows.hasNext()){
				XSSFRow row = (XSSFRow) rows.next();
				JSONObject jsonRow = XSSFRowToJstree(row, headers, false);
				jsonArray = JSONOperator.appendJstree(jsonArray, jsonRow);
			}
			
			json.put("cat", "目錄");
			json.put("type", "default");
			json.put("text", filename.split("\\.")[0]);
			json.put("children", jsonArray);
			workbook.close();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static JSONArray XLSXToSolrDoc(String filepath, String filename){
		
		JSONArray jsonArray = new JSONArray();
		String name = filename.split("\\.")[0];
		try {
			InputStream excelToRead = new FileInputStream(filepath + filename);			
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			int count = 0;
			ArrayList<String> headers = ReadWriteExcelFile.XSSFRowToHeader((XSSFRow) rows.next());
			while(rows.hasNext()){
				
				JSONObject json = new JSONObject();
				XSSFRow row = (XSSFRow) rows.next();
				ArrayList<String> list = ReadWriteExcelFile.XSSFRowToArrayList(row);
				String path = name + "/";
				int i;
				for(i = 0; i < headers.size()-2; ++i){
					path += list.get(i) + "/";
				}
//				json.put("id", count++);
				json.put("path", path);
				json.put("text", list.get(i++));
				json.put("note", list.get(i).equals("") ? "無" : list.get(i));
				
				jsonArray.put(json);
			}
			workbook.close();
		}  catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return jsonArray;
	}
	
	public static void readXLSXFile(String filepath, String filename){
			
		try {
			InputStream excelToRead = new FileInputStream(filepath + filename);			
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			Iterator<Row> rows = sheet.rowIterator();
			
			while(rows.hasNext()){
				
				XSSFRow row = (XSSFRow) rows.next();
				Iterator<Cell> cells = row.cellIterator();
				while(cells.hasNext()){
					
					XSSFCell cell = (XSSFCell) cells.next();
					if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
						System.out.print(cell.getStringCellValue() + " ");
					} else if(cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC){
						System.out.print(cell.getNumericCellValue() + " ");
					} else {
						// System.out.println("neither numeric nor string: ");
					}
				}
				System.out.println();
			}
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}

		
	public static void main(String[] args) {
		
		String filepath = "./";
		String filename = "本草綱目.xlsx";
		JSONObject json = ReadWriteExcelFile.XLSXToJSON(filepath, filename);
//		JSONObject jstree = ReadWriteExcelFile.XLSXToJstreeJSON(filepath, filename);
//		JSONArray jsonArray = ReadWriteExcelFile.XLSXToSolrDoc(filepath, filename);
		PrintWriter writer = null, writer2 = null;
		
		System.out.println(json);
		try {
			writer = new PrintWriter("authority_jstree.json", "UTF-8");
//			writer.println(jstree.toString());
			
			writer2 = new PrintWriter("authority_solrDoc.json", "UTF-8");
//			writer2.println(jsonArray.toString());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally{
			writer.close();
			writer2.close();
		}
	    
	}

}
