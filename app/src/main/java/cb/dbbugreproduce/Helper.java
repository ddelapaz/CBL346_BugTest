package cb.dbbugreproduce;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class Helper {

    private ObjectMapper jacksonMapper;

    public Helper(){
        jacksonMapper = new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    public Boolean DoesPrimaryFolderExist(){
        String PrimaryFolderPath = new Globals().GetPrimaryDirectoryPath();
        return this.Exists(PrimaryFolderPath);
    }

    public String ObjectToJson(Object objData, Class classType) throws Exception{
        String jsonRaw = jacksonMapper.writeValueAsString(objData);
        return jsonRaw;
    }

    public String ObjectToJson(Object objData, TypeReference classType) throws Exception{
        String jsonRaw = jacksonMapper.writeValueAsString(objData);
        return jsonRaw;
    }

    public <T> T JsonToObject(String jsonText, TypeReference classType) throws Exception{
        return (T) jacksonMapper.readValue(jsonText, classType);
    }


    public String decodeUTF8(byte[] bytes) {
        return new String(bytes, Data.UTF8_CHARSET);
    }

    public boolean DeleteContentOfFolder(String fullDirectoryPath){
        try{
            FileUtils.deleteDirectory(new File(fullDirectoryPath));
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public boolean CreateDirectory(String directoryPath){
        try{
            FileUtils.forceMkdir(new File(directoryPath));
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public boolean Exists(String fullFilePath){
        File f = new File(fullFilePath);
        return f.exists();
    }
}
