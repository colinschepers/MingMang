package mingmang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Colin
 */
public class Tools { 
    public void save(Object object, String name){
        try{
            FileOutputStream f_out = new FileOutputStream(name);
            ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
            obj_out.writeObject(object);
            f_out.close();
        } catch (Exception e) { System.out.println(e); }
        System.out.println("Saved " + name);
    }

    public Object load(String name){
        File file = new File(name);
        if(file.exists()){
            try {
                Object obj;
                FileInputStream f_in = new FileInputStream(name);
                ObjectInputStream obj_in = new ObjectInputStream (f_in);
                obj = obj_in.readObject();
                System.out.println("Loaded " + name);
                f_in.close();
                return obj;
            } 
            catch (Exception e) {  e.printStackTrace();}
        }
        return null;
    }
}
