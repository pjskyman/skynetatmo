package sky.netatmo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class Device implements Measurer
{
    private final User user;
    private final String id;
    private final JsonObject attributes;

    Device(User user,String id,JsonObject attributes)
    {
        this.user=user;
        this.id=id;
        this.attributes=attributes;
    }

    public User getUser()
    {
        return user;
    }

    public MeasurementType[] getCompatibleMeasurementTypes()
    {
        try
        {
            JsonObject object=attributes.getAsJsonObject("last_data_store");
            JsonObject lastDataStore=object.getAsJsonObject(id);
            ArrayList<MeasurementType> measurementTypes=new ArrayList<>();
            for(MeasurementType measurementType:MeasurementType.values())
                if(!measurementType.getJSONCode().isEmpty()&&lastDataStore.has(measurementType.getJSONCode()))
                    measurementTypes.add(measurementType);
            return measurementTypes.toArray(new MeasurementType[measurementTypes.size()]);
        }
        catch(JsonParseException e)
        {
            return new MeasurementType[0];
        }
    }

    MeasurementType[] getCompatibleMeasurementTypes(Module module)
    {
        if(module.getDevice()!=this)
            return new MeasurementType[0];
        try
        {
            JsonObject object=attributes.getAsJsonObject("last_data_store");
            JsonObject lastDataStore=object.getAsJsonObject(module.getId());
            ArrayList<MeasurementType> measurementTypes=new ArrayList<>();
            for(MeasurementType measurementType:MeasurementType.values())
                if(!measurementType.getJSONCode().isEmpty()&&lastDataStore.has(measurementType.getJSONCode()))
                    measurementTypes.add(measurementType);
            return measurementTypes.toArray(new MeasurementType[measurementTypes.size()]);
        }
        catch(JsonParseException e)
        {
            return new MeasurementType[0];
        }
    }

    public String getStationName()
    {
        try
        {
            return attributes.getAsJsonPrimitive("station_name").getAsString();
        }
        catch(JsonParseException e)
        {
            return "?";
        }
    }

    public String getId()
    {
        return id;
    }

    public String[] getModuleIds()
    {
        try
        {
            JsonArray moduleArray=attributes.getAsJsonArray("modules");
            String[] moduleIds=new String[moduleArray.size()];
            for(int j=0;j<moduleIds.length;j++)
                moduleIds[j]=moduleArray.get(j).getAsString();
            return moduleIds;
        }
        catch(JsonParseException e)
        {
            return new String[0];
        }
    }

    public String getDeviceName()
    {
        try
        {
            return attributes.get("module_name").getAsString();
        }
        catch(JsonParseException e)
        {
            return "?";
        }
    }

    public String getName()
    {
        return getDeviceName();
    }

    public JsonObject getAttributes()
    {
        return attributes;
    }

    public Module[] getModules() throws NetatmoException
    {
        JsonObject response;
        try
        {
            response=Token.doURLRequest("GET","https://api.netatmo.net/api/devicelist","access_token="+user.getToken().getAccessTokenString());
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
            JsonArray array=response.getAsJsonArray("modules");
            ArrayList<Module> modules=new ArrayList<>();
            for(int i=0;i<array.size();i++)
            {
                JsonObject object=array.get(i).getAsJsonObject();
                if(!object.getAsJsonPrimitive("main_device").getAsString().equals(id))
                    continue;
                modules.add(new Module(this,object.getAsJsonPrimitive("_id").getAsString(),object));
            }
            return modules.toArray(new Module[modules.size()]);
        }
        catch(JsonParseException e)
        {
            System.out.println("modulesHttpResponse="+response.toString());
            throw new NetatmoException("Unreadable response",e);
        }
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
        String arguments="access_token="+user.getToken().getAccessTokenString()+"&device_id="+id+"&scale="+measurementScale.getInputString()+"&type="+types;
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
                        measures.add(new Measure(this,new Date(date),measurementTypes[k],value.get(k).isJsonNull()?Double.NaN:value.get(k).getAsDouble()));
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
