

import com.mongodb.client.model.geojson.Position;

public class ObjectStoredInHashMap {
    private Integer TF;
    private Float TFInPercentage;
    private int position;
    private int place;
    private double idf;

    public void setTF(int x)
    {
        TF=x;
    }
    public int getTF()
    {
        return TF;
    }
    public void setTFInPercentage(Float x)
    {
        TFInPercentage=x;
    }
    public float getTFInPercentage()
    {
        return TFInPercentage;
    }
    public void setPosition(int x)
    {
        position =x;
    }
    public int getPosition()
    {
        return position;
    }
    public void setPlace(int x)
    {
        place =x;
    }
    public int getPlace()
    {
        return place;
    }
    public void setidf(double x)
    {
        idf=x;
    }
    public double getidf()
    {
        return idf;
    }


}
