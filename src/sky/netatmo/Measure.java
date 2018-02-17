package sky.netatmo;

import java.util.ArrayList;
import java.util.Date;

public interface Measure extends Comparable<Measure>
{
    public Measurer getMeasurer();

    public Date getDate();

    public MeasurementType getMeasurementType();

    public double getValue();

    public static Measure[] filter(Measure[] measures,MeasurementType measurementType)
    {
        ArrayList<Measure> list=new ArrayList<>();
        for(Measure measure:measures)
            if(measure.getMeasurementType()==measurementType)
                list.add(measure);
        return list.toArray(new Measure[list.size()]);
    }
}
