package sky.netatmo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Token
{
    private final String accessTokenString;
    private final String refreshTokenString;
    private final String clientId;
    private final String clientSecret;
    private final JsonObject attributes;
    private Token replacement;
    private static final String LINE_SEPARATOR;

    static
    {
        String lineSeparator;
        try
        {
            lineSeparator=System.getProperty("line.separator");
        }
        catch(Exception e)
        {
            lineSeparator="\n";
        }
        LINE_SEPARATOR=lineSeparator;
    }

    private Token(String accessTokenString,String refreshTokenString,String clientId,String clientSecret,JsonObject attributes)
    {
        this.accessTokenString=accessTokenString;
        this.refreshTokenString=refreshTokenString;
        this.clientId=clientId;
        this.clientSecret=clientSecret;
        this.attributes=attributes;
        replacement=null;
    }

    public String getAccessTokenString()
    {
        if(replacement==null)
            return accessTokenString;
        else
            return replacement.getAccessTokenString();
    }

    public User[] getUsers() throws NetatmoException
    {
        JsonObject response;
        try
        {
            response=doURLRequest("GET","https://api.netatmo.net/api/getuser","access_token="+accessTokenString);
        }
        catch(JsonParseException e)
        {
            throw new NetatmoException("Unreadable response",e);
        }
        catch(IOException e)
        {
            if(e.getMessage().contains("Server returned HTTP response code: 403"))
                throw new TokenExpiredException();
            else
                throw new NetatmoException("Unknown error during request",e);
        }
        try
        {
            response=response.getAsJsonObject("body");
            User user=new User(this,response.getAsJsonPrimitive("_id").getAsString(),response);
            return new User[]{user};
        }
        catch(JsonParseException e)
        {
            System.out.println("usersHttpResponse="+response.toString());
            throw new NetatmoException("Unreadable response",e);
        }
    }

    public User getUser() throws NetatmoException
    {
        return getUsers()[0];
    }

    public JsonObject getAttributes()
    {
        return attributes;
    }

    public boolean isExpired()
    {
        try
        {
            doURLRequest("GET","https://api.netatmo.net/api/getuser","access_token="+accessTokenString);
        }
        catch(JsonParseException e)
        {
            return false;
        }
        catch(IOException e)
        {
            return e.getMessage().contains("Server returned HTTP response code: 403");
        }
        return false;
    }

    public Token renewExpiredToken() throws NetatmoException
    {
        JsonObject response;
        try
        {
            response=doURLRequest("POST","https://api.netatmo.net/oauth2/token","grant_type=refresh_token&refresh_token="+refreshTokenString+"&client_id="+clientId+"&client_secret="+clientSecret);
        }
        catch(JsonParseException e)
        {
            throw new NetatmoException("Unreadable response",e);
        }
        catch(IOException e)
        {
            if(e.getMessage().contains("Server returned HTTP response code: 400"))
                throw new NetatmoException("Invalid login data");
            else
                throw new NetatmoException("Unknown error during request",e);
        }
        try
        {
            Token newToken=new Token(response.getAsJsonPrimitive("access_token").getAsString(),response.getAsJsonPrimitive("refresh_token").getAsString(),clientId,clientSecret,response);
            replacement=newToken;
            return newToken;
        }
        catch(JsonParseException e)
        {
            System.out.println("newTokenHttpResponse="+response.toString());
            throw new NetatmoException("Unreadable response",e);
        }
    }

    public static Token getToken(String clientId,String clientSecret,String userName,String password) throws NetatmoException
    {
        JsonObject response;
        try
        {
            response=doURLRequest("POST","https://api.netatmo.com/oauth2/token","grant_type=password&client_id="+clientId+"&client_secret="+clientSecret+"&username="+userName+"&password="+password+"&scope=read_station read_thermostat");
        }
        catch(JsonParseException e)
        {
            throw new NetatmoException("Unreadable response",e);
        }
        catch(IOException e)
        {
            if(e.getMessage().contains("Server returned HTTP response code: 400"))
                throw new NetatmoException("Invalid login data");
            else
                throw new NetatmoException("Unknown error during request",e);
        }
        try
        {
            return new Token(response.getAsJsonPrimitive("access_token").getAsString(),response.getAsJsonPrimitive("refresh_token").getAsString(),clientId,clientSecret,response);
        }
        catch(JsonParseException e)
        {
            System.out.println("tokenHttpResponse="+response.toString());
            throw new NetatmoException("Unreadable response",e);
        }
    }

    public static JsonObject doURLRequest(String method,String url,String parameters) throws IOException,JsonParseException
    {
        HttpURLConnection connection=null;
        try
        {
            StringBuilder stringBuilder=new StringBuilder();
            connection=(HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod(method);
            connection.setAllowUserInteraction(false);
            connection.setDoOutput(true);
            try(PrintWriter printWriter=new PrintWriter(connection.getOutputStream()))
            {
                printWriter.print(parameters);
                printWriter.flush();
            }
            BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while((line=bufferedReader.readLine())!=null)
            {
                stringBuilder.append(line);
                stringBuilder.append(LINE_SEPARATOR);
            }
            connection.disconnect();
//            System.out.println("requestResponse="+stringBuilder.toString());
            return new JsonParser().parse(stringBuilder.toString()).getAsJsonObject();
        }
        finally
        {
            if(connection!=null)
                connection.disconnect();
        }
    }
}
