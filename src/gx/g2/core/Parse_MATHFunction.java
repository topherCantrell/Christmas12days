package gx.g2.core;

public enum Parse_MATHFunction 
{	
	
	MULT("*"), DIV("/"), REM("%");
	
	String token;
	
	Parse_MATHFunction(String token)
	{
		this.token = token;		
	}
	
	public String getToken()
	{
		return token;
	}

}
