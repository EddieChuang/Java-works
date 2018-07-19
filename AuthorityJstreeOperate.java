
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;


public class AuthorityJstreeOperate {
	
	private String filePath = null;
	private String fileName = null;
	private String authorityId = null;
	private JstreeNode jstree = null;
//	private String targetNodePath = "";
	
	public AuthorityJstreeOperate(String filePath, String fileName, String authorityId){
		
		this.filePath = filePath;
		this.authorityId = authorityId;
		this.fileName = fileName;
		init();
	}
	
	public AuthorityJstreeOperate(JSONObject jstree, String filePath, String fileName, String authorityId){
		
		this.filePath = filePath;
		this.authorityId = authorityId;
		this.fileName = fileName;
		this.jstree = new JstreeNode(jstree);
	}
	
	public JstreeNode getJstree(){
		return this.jstree;
	}
	
	private void init(){
		
		String jstree_str = "";
		String line = "";
		InputStreamReader isr = null;
		BufferedReader br = null;
		try{
			isr = new InputStreamReader(new FileInputStream(new File(this.filePath + this.fileName)), "UTF-8");
			br = new BufferedReader(isr);
			while((line = br.readLine()) != null){
				jstree_str += line;
			}
			isr.close();
			br.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		this.jstree = new JstreeNode(new JSONObject(jstree_str));
	}
	
//	public void setRootText(String newText){
//		JSONObject jstree = this.readJstreeFile();
//		jstree.put("text", newText);
//		this.write(jstree);
//	}
	
	public void appendNode(JstreeNode node){
		this.jstree.append(node);
	}
	
	/** 回傳該路徑的節點 */
	public JstreeNode getJstreeNode(String path){
		
		JstreeNode root = new JstreeNode(this.jstree); // 複製新的 jstree
		ArrayList<String> pathList = new ArrayList<String>(Arrays.asList(path.split("/")));
		if(!pathList.get(0).equals(root.getText())){
			return null;
		}
		pathList.remove(0);
		return getJstreeNodeHelper(root, pathList);
	}
	
	private JstreeNode getJstreeNodeHelper(JstreeNode node, ArrayList<String> pathList){
		
		if(pathList.size() == 0){
			return node;
		}
		String text = pathList.get(0);
		ArrayList<JstreeNode> children = node.getChildren();
		for(JstreeNode child : children){
			if(child.getText().equals(text)){
				pathList.remove(0);
				return getJstreeNodeHelper(child, pathList);
			}
		}
		return null;
	}
	
	public JstreeNode getTargetNode(JstreeNode node){
		
		if(node.isTarget()){
			return node;
		}
		JstreeNode target = null;
		ArrayList<JstreeNode> children = node.getChildren();
		for(int i = 0; i < children.size() && target==null; ++i){
			JstreeNode child = children.get(i);
			target = getTargetNode(child); 
		}
		return target;
	}
	
	public String getTargetNodePath(JstreeNode node, String path){
		
		path += node.getText() + "/";
		if(node.isTarget()){
			return path;
		}
		
		String targetPath = "";
		ArrayList<JstreeNode> children = node.getChildren();
		for(int i = 0; i < children.size() && targetPath.equals(""); ++i){
			JstreeNode child = children.get(i);
			targetPath = getTargetNodePath(child, path); 
		}
		return targetPath;
	}
	
	/** 設定新目標目錄，取消舊目標目錄 */
	public void setTargetNode(String path){                                                                                                                                                                                                                                                                                      
		
		JstreeNode oldTarget = this.getTargetNode(this.getJstree());
		JstreeNode newTarget = this.getJstreeNode(path);
		if(oldTarget != null) {
			oldTarget.setTarget(false);
		}
		newTarget.setTarget(true);
//		JSONObject json = new JSONObject();
	}
	
	
	
	/**
	 *  輸出檔案
	 * */
	public void write(){
		
		try {
			PrintWriter pw = new PrintWriter(this.filePath + this.fileName, "UTF-8");
			pw.println(this.jstree.toJSON().toString());
			pw.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	public static void write(String filePath, String fileName, JSONObject jstree){
		try {
			PrintWriter pw = new PrintWriter(filePath + fileName, "UTF-8");
			pw.println(jstree.toString());
			pw.close();
		} catch (FileNotFoundException |UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
//		afo.setRootText("newText");
//		JSONObject jstree = ajo.readJstreeFile();
////		for(String key : jstree.keySet()){
////			System.out.println(key + ": " + jstree.get(key));
////		}
//		
//		String authorityId2 = "314";
//		String fileName2 = authorityId2 + "_jstree.json";
//		AuthorityJstreeOperate afo2 = new AuthorityJstreeOperate(filePath, fileName2, authorityId2);
//		afo2.write(jstree);
	}

}
