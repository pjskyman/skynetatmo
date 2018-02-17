package sky.netatmo;

import com.google.gson.JsonObject;

public interface Device extends Measurer
{
    public MeasurementType[] getCompatibleMeasurementTypes(Module module);

    public String getStationName();

    public String[] getModuleIds();

    public String getDeviceName();

    public JsonObject getAttributes();

    public Module[] getModules() throws NetatmoException;
}
