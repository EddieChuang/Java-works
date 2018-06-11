import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

public class AuthorityFileUploadException extends Exception{
	
	private ArrayList<Map<String, String>> error_data;
	private ArrayList<String> header;
	private ArrayList<String> error_msg;
	private boolean hasException;
	
	public AuthorityFileUploadException(){
		super("權威檔格式錯誤");
		this.error_data = new ArrayList<Map<String, String>>();
		this.header     = new ArrayList<String>();
		this.error_msg  = new ArrayList<String>();
		this.hasException = false;
	}

	
	public void appendErrorData(Map<String, String> data){
		this.error_data.add(data);
	}
	
	public void appendErrorMessage(String msg){
		this.error_msg.add(msg);	
	}
	
	public void setHeader(ArrayList<String> header){
		this.header = header;
	}
	
	public void setHasException(boolean hasException){
		this.hasException = hasException;
	}
	
	public ArrayList<Map<String, String>> getErrorData(){
		return this.error_data;
	}
	
	public ArrayList<String> getHeader(){
		return this.header;
	}
	
	public ArrayList<String> getErrorMessage(){
		return this.error_msg;
	}
	
	public boolean getHasException(){
		return this.hasException;
	}
	
}
