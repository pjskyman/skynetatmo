package sky.netatmo;

public class TokenExpiredException extends NetatmoException
{
    public TokenExpiredException()
    {
        super("Token has expired");
    }
}
