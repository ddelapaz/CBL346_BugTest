package cb.dbbugreproduce.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class CadecDataModel {

    @JsonProperty("outofstockdelivery") //Delivery list
    public List<ManifestDelivery> OutOfStockDelivery = new ArrayList<>();
}
