package sky.netatmo;

import com.google.gson.JsonObject;

public interface Module extends Measurer
{
    public Device getDevice();

    public String getModuleName();

    public String getType();

    public JsonObject getAttributes();
}
