package sky.netatmo;

import java.util.Date;

public interface Measurer
{
    public static final Date LAST_DATE=new Date(1L);

    public String getName();

    public String getId();

    public User getUser();

    public MeasurementType[] getCompatibleMeasurementTypes();

    public Measure[] getMeasures(MeasurementScale measurementScale,Date beginDate,Date endDate,MeasurementType... measurementTypes) throws NetatmoException;
}
