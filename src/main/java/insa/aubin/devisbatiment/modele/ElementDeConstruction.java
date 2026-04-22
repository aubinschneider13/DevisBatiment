package insa.aubin.devisbatiment.modele;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class ElementDeConstruction {
    private String id;
    public ElementDeConstruction(String prefixe) {
        this.id = generetId(prefixe);
    }
    public String getId() {
        return id;
    }
    public String generetId(String prefixe) {
        return prefixe + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());
    }

    @Override
    public abstract String toString();
}
