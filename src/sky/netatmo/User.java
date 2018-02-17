package sky.netatmo;

import com.google.gson.JsonObject;
import java.util.Date;
import java.util.Locale;

public interface User
{
    public Token getToken();

    public String getId();

    public String getMail();

    public Date getCreationDate();

    public Locale getLocale();

    public JsonObject getAttributes();

    public Device[] getDevices() throws NetatmoException;
}
