package sky.netatmo;

public class NetatmoException extends Exception
{
    public NetatmoException()
    {
    }

    public NetatmoException(String message)
    {
        super(message);
    }

    public NetatmoException(Throwable cause)
    {
        super(cause);
    }

    public NetatmoException(String message,Throwable cause)
    {
        super(message,cause);
    }
}
