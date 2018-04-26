package cs6378.message;

public interface MsgType {
	String LOGIN = "login";
	String LOGIN_SUCCESS = "login_success";
	String REPLY = "reply";
	String REQUEST = "request";
	String HEARTBEAT = "heartbeat";
	String CREATE = "create";
	String APPEND = "append";
	String READ = "read";
	String FAILURE = "failure";
	
	String CHUNK_DELIMITER = "chunk_delimiter";
}
