package sky.netatmo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class DefaultModule implements Module
{
    private final Device device;
    private final String id;
    private final JsonObject attributes;

    DefaultModule(Device device,String id,JsonObject attributes)
    {
        this.device=device;
        this.id=id;
        this.attributes=attributes;
    }

    public Device getDevice()
    {
        return device;
    }

    public String getId()
    {
        return id;
    }

    public String getModuleName()
    {
        try
        {
            return attributes.getAsJsonPrimitive("module_name").getAsString();
        }
        catch(JsonParseException e)
        {
            return "?";
        }
    }

    public String getName()
    {
        return getModuleName();
    }

    public User getUser()
    {
        return device.getUser();
    }

    public MeasurementType[] getCompatibleMeasurementTypes()
    {
        return device.getCompatibleMeasurementTypes(this);
    }

    public String getType()
    {
        try
        {
            return attributes.getAsJsonPrimitive("type").getAsString();
        }
        catch(JsonParseException e)
        {
            return "?";
        }
    }

    public JsonObject getAttributes()
    {
        return attributes;
    }

    public Measure[] getMeasures(MeasurementScale measurementScale,Date beginDate,Date endDate,MeasurementType... measurementTypes) throws NetatmoException
    {
        if(measurementScale==null)
            throw new IllegalArgumentException("A null scale of measurement has been specified");
        if(endDate==LAST_DATE&&beginDate!=null)
            throw new IllegalArgumentException("You must specify a null begin date if you want only the last measurement");
        if(measurementTypes.length==0)
            throw new IllegalArgumentException("You must specify at least one type of measurement");
        for(MeasurementType measurementType:measurementTypes)
        {
            if(measurementType==null)
                throw new IllegalArgumentException("A null type of measurement has been specified");
            if(!measurementType.isCompatibleWith(measurementScale))
                throw new IllegalArgumentException("The type of measurement "+measurementType+" is incompatible with the scale of measurement "+measurementScale);
        }
        for(int i=1;i<measurementTypes.length;i++)
        {
            for(int j=0;j<i;j++)
                if(measurementTypes[j]==measurementTypes[i])
                    throw new IllegalArgumentException("A type of measurement has been specified at least two times");
        }
        String types="";
        for(int i=0;i<measurementTypes.length;i++)
        {
            types+=measurementTypes[i].getInputString();
            if(i<measurementTypes.length-1)
                types+=",";
        }
        String beginDateString=null;
        if(beginDate!=null)
            beginDateString=""+beginDate.getTime()/1000L;
        String endDateString=null;
        if(endDate!=null)
            if(endDate==LAST_DATE)
                endDateString="last";
            else
                endDateString=""+endDate.getTime()/1000L;
        String arguments="access_token="+device.getUser().getToken().getAccessTokenString()+"&device_id="+device.getId()+"&module_id="+id+"&scale="+measurementScale.getInputString()+"&type="+types;
        if(beginDate!=null)
            arguments+="&date_begin="+beginDateString;
        if(endDate!=null)
            arguments+="&date_end="+endDateString;
        JsonObject response;
        try
        {
            response=Token.doURLRequest("GET","https://api.netatmo.net/api/getmeasure",arguments);
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
            ArrayList<Measure> measures=new ArrayList<>();
            JsonArray array=response.getAsJsonArray("body");
            for(int i=0;i<array.size();i++)
            {
                JsonObject object=array.get(i).getAsJsonObject();
                long date=object.getAsJsonPrimitive("beg_time").getAsLong()*1000L;
                JsonArray values=object.getAsJsonArray("value");
                for(int j=0;j<values.size();j++)
                {
                    JsonArray value=values.get(j).getAsJsonArray();
                    for(int k=0;k<value.size();k++)
                        measures.add(new DefaultMeasure(this,new Date(date),measurementTypes[k],value.get(k).isJsonNull()?Double.NaN:value.get(k).getAsDouble()));
                    if(object.has("step_time"))
                        date+=object.getAsJsonPrimitive("step_time").getAsLong()*1000L;
                }
            }
            return measures.toArray(new Measure[measures.size()]);
        }
        catch(JsonParseException e)
        {
            throw new NetatmoException("Unreadable response",e);
        }
    }
}
