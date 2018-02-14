package sky.netatmo;

import java.util.Date;
import java.util.GregorianCalendar;

public class Test
{
    public static void main(String[] args)
    {
        try
        {
            Token token=Token.getToken("123456...","biloute...","mail@mail.com","password");
            User user=token.getUser();
            Device[] devices=user.getDevices();
            Date date=new GregorianCalendar(2012,10,15,0,0,0).getTime();
            Measure[] measures=devices[0].getMeasures(MeasurementScale.MAX,date,null,MeasurementType.TEMPERATURE);
            for(Measure measure:measures)
                System.out.println(measure);
            System.out.println();
            Module[] modules=devices[0].getModules();
            measures=modules[0].getMeasures(MeasurementScale.MAX,date,null,MeasurementType.TEMPERATURE);
            for(Measure measure:measures)
                System.out.println(measure);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
