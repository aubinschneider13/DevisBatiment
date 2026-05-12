package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class ElementDeConstruction {
    
    private String id;
    
    public ElementDeConstruction(String prefixe) {
        this.id = genererId(prefixe);
    }
    public String getId() {
        return id;
    }
    public String genererId(String prefixe) {
        return prefixe + "_" + java.util.UUID.randomUUID().toString();
    }

    public abstract String toCSV();

    @Override
    public abstract String toString();
}
