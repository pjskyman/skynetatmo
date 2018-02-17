package sky.netatmo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public class DefaultUser implements User
{
    private final Token token;
    private final String id;
    private final JsonObject attributes;

    DefaultUser(Token token,String id,JsonObject attributes)
    {
        this.token=token;
        this.id=id;
        this.attributes=attributes;
    }

    public Token getToken()
    {
        return token;
    }

    public String getId()
    {
        return id;
    }

    public String getMail()
    {
        try
        {
            return attributes.getAsJsonPrimitive("mail").getAsString();
        }
        catch(JsonParseException e)
        {
            return "?";
        }
    }

    public Date getCreationDate()
    {
        try
        {
            return new Date(attributes.getAsJsonObject("date_creation").getAsJsonPrimitive("sec").getAsLong()*1000L);
        }
        catch(JsonParseException e)
        {
            return new Date(0L);
        }
    }

    public Locale getLocale()
    {
        try
        {
            String locale=attributes.getAsJsonObject("administrative").getAsJsonPrimitive("lang").getAsString();
            return new Locale(locale.substring(0,2),locale.substring(3,5));
        }
        catch(JsonParseException e)
        {
            return Locale.getDefault();
        }
    }

    public JsonObject getAttributes()
    {
        return attributes;
    }

    public Device[] getDevices() throws NetatmoException
    {
        JsonObject response;
        try
        {
            response=Token.doURLRequest("GET","https://api.netatmo.net/api/devicelist","access_token="+token.getAccessTokenString());
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
            JsonArray array=response.getAsJsonArray("devices");
            Device[] devices=new Device[array.size()];
            for(int i=0;i<devices.length;i++)
            {
                JsonObject object=array.get(i).getAsJsonObject();
                JsonArray moduleArray=object.getAsJsonArray("modules");
                String[] moduleIds=new String[moduleArray.size()];
                for(int j=0;j<moduleIds.length;j++)
                    moduleIds[j]=moduleArray.get(j).getAsJsonPrimitive().getAsString();
                devices[i]=new DefaultDevice(this,object.get("_id").getAsJsonPrimitive().getAsString(),object);
            }
            return devices;
        }
        catch(JsonParseException e)
        {
            System.out.println("devicesHttpResponse="+response.toString());
            throw new NetatmoException("Unreadable response",e);
        }
    }
}
