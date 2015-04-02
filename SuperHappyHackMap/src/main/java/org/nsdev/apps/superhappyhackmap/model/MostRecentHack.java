package org.nsdev.apps.superhappyhackmap.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by neal 13-03-14 4:20 PM
 */
@DatabaseTable(tableName = "MostRecentHack")
public class MostRecentHack {

    @DatabaseField(id = true, canBeNull = false)
    private int id;

    public static final String LAST_HACKED_FIELD_NAME = "hackTime";
    @DatabaseField(columnName = LAST_HACKED_FIELD_NAME, dataType = DataType.DATE_LONG)
    private Date hackTime;

    public MostRecentHack() {
        // Required
    }

    public MostRecentHack(Date hackTime) {
        this.id = 0;
        this.hackTime = hackTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getHackTime() {
        return hackTime;
    }

    public void setHackTime(Date hackTime) {
        this.hackTime = hackTime;
    }
}
