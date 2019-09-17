package cb.dbbugreproduce.models.eventbus;

public class EventBusPostTextviewUpdate {
    private String Text;

    public EventBusPostTextviewUpdate(final String text){
        this.Text = text;
    }

    public String GetText(){
        return this.Text;
    }
}
