package jsonOperate;

/*** Dependency Library ***/
// java-json.jar

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONOperator {
	
	public static JSONArray append(JSONArray jsonArray, JSONObject jsonToAppend){
		
		if(jsonToAppend == null || jsonToAppend.length() == 0)
			return jsonArray;
		
		try {
				String cat  = jsonToAppend.getString("cat");
				String text = jsonToAppend.getString("text"); 
				for(int i = 0; i < jsonArray.length(); ++i){
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						if(jsonObject.getString("cat").equals(cat) && jsonObject.getString("text").equals(text)){
								JSONArray children = jsonObject.getJSONArray("children");
								if(children.length() == 0){
										jsonArray.put(jsonToAppend);
										return jsonArray;
								} else {
									
										jsonObject.put("children", append(children, jsonToAppend.getJSONArray("children").getJSONObject(0)));
										jsonArray.put(i, jsonObject);
										return jsonArray;
								}
						}
				}
		} catch (JSONException e) {
				e.printStackTrace();
		}

		jsonArray.put(jsonToAppend);
		return jsonArray;
	}
	
public static JSONArray appendJstree(JSONArray jsonArray, JSONObject jsonToAppend){
		
		if(jsonToAppend == null || jsonToAppend.length() == 0)
				return jsonArray;
		try {
				String cat  = jsonToAppend.getString("cat");
				String text = jsonToAppend.getString("text"); 
				for(int i = 0; i < jsonArray.length(); ++i){
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						if(jsonObject.getString("cat").equals(cat) && jsonObject.getString("text").equals(text)){
								JSONArray children = jsonObject.getJSONArray("children");
								if(children.length() == 0){
										return jsonArray;
								} else {
										jsonObject.put("children", appendJstree(children, jsonToAppend.getJSONArray("children").getJSONObject(0)));
										jsonArray.put(i, jsonObject);
										return jsonArray;
								}
						}
				}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		jsonArray.put(jsonToAppend);
		return jsonArray;
	}

	

	public static void main(String[] args) {

			try {
				JSONObject json = new JSONObject("{a:1, a:2, a:3}");
					
				System.out.println(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	}
}
