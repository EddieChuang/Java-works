import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JstreeNode {
	
	private String  text = "";   
	private String  type = "";  // = ['default', 'subDir']
	private JSONObject data = new JSONObject(); // default: {remove: true, rename: true}, 可以刪除  & 可以更名
	private ArrayList<JstreeNode> children = new ArrayList<JstreeNode>();
	
	public JstreeNode(){
		this.data.put("remove", true);  // 可以刪除
		this.data.put("rename", true);  // 可以更名
		this.data.put("target", false);
	}
	
	// copy contructor
	public JstreeNode(JstreeNode node){
		this.text = new String(node.getText());
		this.type = new String(node.getType());
		this.data = new JSONObject(node.getData());
		this.children = new ArrayList<JstreeNode>(node.getChildren());
	}
	
	public JstreeNode(JSONObject node){
		this.fromJSON(node);
	}
	
	public JstreeNode(String type){
		this.type = type;
		this.data.put("remove", true);
		this.data.put("rename", true);
		this.data.put("target", false);
	}
	
	public JstreeNode(String text, String type, ArrayList<JstreeNode> children){
		this.text = text;
		this.type = type;
		this.children = children;
		this.data.put("remove", true);
		this.data.put("rename", true);
		this.data.put("target", false);
	}
	
	public JstreeNode(String text, String type){
		this.text = text;
		this.type = type;
		this.data.put("remove", true);
		this.data.put("rename", true);
		this.data.put("target", false);
	}
	
	public JstreeNode(String text, String type, boolean remove, boolean rename, boolean target){
		this.text = text;
		this.type = type;
		this.data.put("remove", remove);
		this.data.put("rename", rename);
		this.data.put("target", target);
	}
	
	/*
	 * 新增節點
	 * */
	public void append(JstreeNode newNode){
		if(newNode == null)
			return;
		try {
			String text = newNode.getText();
			for(int i = 0; i < this.children.size(); ++i){
				JstreeNode child = this.children.get(i);
				if(child.getText().equals(text)){ // 找相同名稱的節點
					// 有相同名稱的節點，將 newNode.children 新增到 child
					ArrayList<JstreeNode> newNodeChildren = newNode.getChildren();
					for(int j = 0; j < newNodeChildren.size(); ++j){
						child.append(newNodeChildren.get(j));
					}
					return ;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// 沒有相同名稱的節點，直接新增
		this.children.add(newNode);		
	}
	
	/*
	 * 新增節點
	 * */
	public void append(ArrayList<String> path){
		append(toJstreeNode(path, 0));
	}

	/*
	 * 新增節點
	 * */
	public void append(JSONObject jsonNode){
		
		JstreeNode newNode = new JstreeNode();
		newNode.fromJSON(jsonNode);
		append(newNode);
	}
	
	/*
	 * 階層: ['水部', '天水類', '雨水'] -> JstreeNode
	 * */
	public static JstreeNode toJstreeNode(ArrayList<String> path, int i){
		
		if(i < path.size()){
			String data = path.get(i);
			if(!data.equals("")){
				try {
					JstreeNode node = new JstreeNode(data.trim(), "subDir");
					ArrayList<JstreeNode> children = new ArrayList<JstreeNode>();
					JstreeNode child = toJstreeNode(path, i+1);
					if(child != null)
						children.add(child);
					node.setChildren(children);
					return node;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				return toJstreeNode(path, i+1);
			}
		}
		return null;
	}
	
	/*
	 * 轉換成 JSONObject
	 * */
	public JSONObject toJSON(){
		
		JSONObject jstreeJson = new JSONObject();
		JSONArray  children   = new JSONArray();
		
		for(int i = 0; i < this.children.size(); ++i){
			children.put(this.children.get(i).toJSON());
		}
		
		jstreeJson.put("text", this.text);
		jstreeJson.put("type", this.type);
		jstreeJson.put("data", this.data);
		jstreeJson.put("children", children);
		return jstreeJson;
	}
	
	/*
	 * 從 JSONObject 初始 JstreeNode
	 * */
	public void fromJSON(JSONObject node){
		
		JSONObject init_data = new JSONObject();
		init_data.put("remove", true);
		init_data.put("rename", true);
		init_data.put("target", false);
		
		this.text     = node.getString("text");
		this.type     = node.getString("type");
		this.data     = node.has("data") ? node.getJSONObject("data") : init_data; 
		this.children = new ArrayList<JstreeNode>();
		
		JSONArray children = node.getJSONArray("children");
		for(int i = 0; i < children.length(); ++i){
			JstreeNode child = new JstreeNode();
			child.fromJSON(children.getJSONObject(i));
			this.children.add(child);
		}
	}
	
	public String getText(){
		return this.text;
	}
	
	public String getType(){
		return this.type;
	}
	
	public JSONObject getData(){
		return this.data;
	}
	
	public ArrayList<JstreeNode> getChildren(){
		return this.children;
	}
	
	public boolean isTarget(){
		return this.data.getBoolean("target");
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public void setType(String type){
		this.type = type;
	}

	public void setData(JSONObject data){
		this.data = data;
	}
	
	public void setChildren(ArrayList<JstreeNode> chhildren){
		this.children = chhildren;
	}
	
	public void setTarget(boolean isTarget){
		this.data.put("target", isTarget);
	}
	
	public static void main(String[] args){
		
		/* example 1*/
		JstreeNode jstree = new JstreeNode("本草綱目", "default");
		
		JSONObject node = new JSONObject(); 
		node.put("text", "石部");
		node.put("type", "subDir");
		node.put("children", new JSONArray());
		JstreeNode jstreeNode = new JstreeNode(node);
		jstreeNode.setTarget(true);
		
		ArrayList<String> path1 = new ArrayList<String>();
		path1.add("水部");
		path1.add("天水類");
		path1.add("雨水");
		path1.add("水");
		
		ArrayList<String> path2 = new ArrayList<String>();
		path2.add("水部");
		path2.add("天水類");
		path2.add("雨水");
		
		path2.add("水");
		path2.add("雨雨水");
		
		jstree.append(path1);
		jstree.append(path2);
		jstree.append(node);
		JstreeNode target = jstree.getChildren().get(0);
		target.setTarget(true);
		
		System.out.println(jstree.toJSON());
		/* end of example 1*/
		
		/* example 2*/
//		String jsonString = "{\"data\":{\"rename\":true,\"remove\":true},\"children\":[{\"data\":{\"rename\":true,\"remove\":true},\"children\":[{\"data\":{\"rename\":true,\"remove\":true},\"children\":[{\"data\":{\"rename\":true,\"remove\":true},\"children\":[],\"text\":\"雨水\",\"type\":\"subDir\"},{\"data\":{\"rename\":true,\"remove\":true},\"children\":[],\"text\":\"梅雨水\",\"type\":\"subDir\"}],\"text\":\"天水類\",\"type\":\"subDir\"}],\"text\":\"水部\",\"type\":\"subDir\"},{\"data\":{\"rename\":true,\"remove\":true},\"children\":[],\"text\":\"火部\",\"type\":\"subDir\"}],\"text\":\"本草綱目\",\"type\":\"default\"}";
//		JSONObject json = new JSONObject(jsonString);
//		JstreeNode jstree = new JstreeNode();
//		jstree.fromJSON(json);
//		System.out.println(jstree.toJSON());
//		System.out.println(jstree.toJSON().toString().equals(jsonString));
		/* end of example 2*/
		
//		System.out.println(jstree.toJSON());
//		System.out.println(jsonString);
	}
}
