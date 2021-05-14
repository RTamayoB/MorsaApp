package com.example.morsaapp.data;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Class for the control of the SQLite Database, holds the process for the creation and upgrade
 * of the db as well for the methods that return data to the views.
 */
public class DBConnect extends SQLiteOpenHelper {

    public Context context;
    public DBConnect(@Nullable Context context, @Nullable String name, @Nullable android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db) {
        db.execSQL(OdooData.CREATE_TABLE_ORDERS);
        db.execSQL(OdooData.CREATE_TABLE_ITEMS);
        db.execSQL(OdooData.CREATE_TABLE_STOCK);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_ITEMS);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_ISSUES);
        db.execSQL(OdooData.CREATE_TABLE_ISSUES_LIST);
        db.execSQL(OdooData.CREATE_TABLE_PRODUCT_PRODUCT);
        db.execSQL(OdooData.CREATE_TABLE_USERS);
        db.execSQL(OdooData.CREATE_TABLE_ROUTES);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_BOX);
        db.execSQL(OdooData.CREATE_TABLE_RACK);
        db.execSQL(OdooData.CREATE_TABLE_INVENTORY_LINE);
        db.execSQL(OdooData.CREATE_TABLE_INVOICE);
        db.execSQL(OdooData.CREATE_TABLE_INVOICE_LINE);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_ARRANGEMENT);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_RETURN);
        db.execSQL(OdooData.CREATE_TABLE_STOCK_RETURN_LINE);
        db.execSQL(OdooData.CREATE_TABLE_RES_USERS);
    }

    @Override
    public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {
        try{
            Log.d("OnUpgrade", "Upgrading");
            db.execSQL("ALTER TABLE "+OdooData.TABLE_STOCK+" ADD COLUMN in_inspection TEXT");
            SharedPreferences prefs = context.getSharedPreferences("startupPreferences", 0);
            int ver = prefs.getInt("DBver",1);
            prefs.edit().putInt("DBver",ver+1).apply();
        }catch (Exception e){
            Log.d("Error",e.toString());
        }

    }

    /**
     * Creates a table from the ground up and fill its with data, currently UNUSED
     * @param Json Data to fill the table
     * @param tableName Table to be filled
     * @param parent_id Specifies the primary key
     * @return True if successful
     */
    private boolean createTable(String Json, String tableName, String parent_id){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            //Convert into JSONArray
            JSONArray data = new JSONArray(Json);
            //Get first JSONObject to fill table
            JSONObject tableSetter = data.getJSONObject(0);
            Iterator<String> tableite = tableSetter.keys();
            /*
            if(parent_id.equals("reservation_aid")){
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN reservation_aid TEXT");
            }
            */
            while (tableite.hasNext()){
                String key = tableite.next();
                if(!isFieldExist(key,tableName) ){
                    db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + key + " TEXT");
                }
            }
            db.close();
            //Check if initial config has been made, if not, fill the db with the data
            //If initial config has been made, dont call to the fill method.
            //fillTable(data,tableName);
        }catch (JSONException jsonException){
            Log.d("JSON Error",jsonException.toString());
        }catch (Exception e){
            Log.d("Error", e.toString());
        }
        Log.d("Created","Table Created");
        return true;
    }

    /**
     * Fills the specified table with the provided data
     * @param data The JsonArray with information fetched from Odoo
     * @param tableName The table where you want to put the data
     * @return True if the operation is successful
     * NOTE That this function has to be used after deleteDataOnTable() or deletaDataOnTableFromField,
     * otherwise you are going to have duplicates
     */
    public boolean fillTable(JSONArray data, String tableName){
        try {
            SQLiteDatabase db = getWritableDatabase();
            for (int i = 0; i < data.length(); i++) {
                JSONObject jsonObject = data.getJSONObject(i);
                Iterator<String> keys = jsonObject.keys();
                ContentValues contentValues = new ContentValues();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = jsonObject.get(key).toString();
                    Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
                    cursor.moveToFirst();
                    do {
                        String currentColumn = cursor.getString(1);
                        if (currentColumn.equals(key)) {
                            contentValues.put(key, value);
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                db.insert(tableName, null, contentValues);
            }
            Log.d("Success", "Successful");
            db.close();
        }catch (JSONException jsonException){
            Log.d("JSON Error",jsonException.toString());
        }catch (Exception e){
            Log.d("Error", e.toString());
        }
        return true;
    }

    /**
     * Checks if a field already exits when creating a new table, currently UNUSED
     * @param fieldName The field you want to add
     * @param tableName The table where you want to add the field
     * @return True if the field already exists
     */
    private boolean isFieldExist(String fieldName, String tableName){
        boolean isExist = false;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("PRAGMA table_info("+tableName+")",null);
        cursor.moveToFirst();
        do {
            String currentColumn = cursor.getString(1);
            if(currentColumn.equals(fieldName)) {
                isExist = true;
            }
        }while (cursor.moveToNext());
        cursor.close();
        return isExist;
    }

    /*
     * This are queries to the database to fill mostly the listviews in the views
     */

    public Cursor fillOrdersListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, display_name ,name, date_order, group_id, partner_id, amount_total, partner_id_street, partner_ref FROM "+ OdooData.TABLE_ORDER+" WHERE state = 'purchase'",null);
    }

    public Cursor fillInvoiceListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, display_name, number, datetime_invoice, partner_id, amount_total, origin, purchase_id FROM "+ OdooData.TABLE_INVOICE+" WHERE state = 'open'",null);
    }

    public Cursor fillRefundListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, type_id, partner_id, date, state, amount_total FROM "+ OdooData.TABLE_STOCK_RETURN,null);
    }

    public Cursor getRoutes(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name FROM "+ OdooData.TABLE_ROUTES+"",null);
    }

    public Cursor getRoute(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name FROM "+ OdooData.TABLE_ROUTES+" WHERE name = '"+name+"'", null);
    }

    public Cursor fillIncidenciesListView(boolean isDevolution){
        SQLiteDatabase db = this.getReadableDatabase();
        if(isDevolution){
            return db.rawQuery("SELECT * FROM "+ OdooData.TABLE_STOCK_ISSUES+" WHERE devolution = 'true'",null);
        }
        else{
            return db.rawQuery("SELECT * FROM "+ OdooData.TABLE_STOCK_ISSUES+" WHERE issue = 'true'",null);
        }
    }

    public Cursor fillItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT display_name, product_qty, price_unit, price_total FROM "+ OdooData.TABLE_ITEMS+" WHERE order_id ='"+subId+"'",null);
    }

    public Cursor fillInvoiceItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT display_name, quantity, price_unit, price_subtotal FROM "+ OdooData.TABLE_INVOICE_LINE+" WHERE invoice_id ='"+subId+"'",null);
    }

    public Cursor fillRefundItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT name, id , product_id , price_unit , qty, return_id FROM "+ OdooData.TABLE_STOCK_RETURN_LINE+" WHERE return_id ='"+subId+"'",null);
    }

    public Cursor fillStockListView(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, name, group_id, partner_id, date_done, origin_invoice_purchase, return_id, origin FROM "+ OdooData.TABLE_STOCK+" /*WHERE state = 'done' AND group_id != 'false' AND origin_invoice_purchase != ''*/ ORDER BY origin_invoice_purchase ASC",null); //group_id != false is temporal fix
    }

    public Cursor getStockBoxesFromRoute(String route){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, route, invoices, box FROM "+ OdooData.TABLE_STOCK_BOX+" WHERE route = '"+route+"' AND is_scanned = 'false'",null);
    }

    public Cursor fillPickingsListView(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, name, group_id, partner_id, date_done FROM "+ OdooData.TABLE_STOCK+" WHERE id in "+list+"",null);
    }

    public Cursor fillLocationsListView(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, partner_id, name, folio, num_products FROM "+ OdooData.TABLE_STOCK_ARRANGEMENT,null);
    }

    public Cursor movesTest(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE id in "+list,null);
    }

    public Cursor getLocationMoves(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id, product_description FROM "+ OdooData.TABLE_STOCK_ITEMS+" /*WHERE picking_originative_id = ?*/ ORDER BY location_dest_id ASC",null);
    }

    public Cursor movesTestReStock(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id, product_description FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE state != 'cancel'",null);
    }

    public Cursor pickingMovesTest(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+list+"'",null);
    }

    public Cursor pickingMovesById(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, transfer_qty, product_id, id, picking_id, location_id, product_description, name FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE id = '"+list+"'",null);
    }

    public Cursor getPickingMoveIds(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+list+"'",null);
    }


    public Cursor pickingMovesInIds(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE picking_id in "+list+"",null);
    }

    public Cursor getNameFromPicking(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT name FROM "+ OdooData.TABLE_STOCK+" WHERE id = '"+id+"'",null);
    }

    public Cursor getStockStatefromOrder(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT state FROM "+ OdooData.TABLE_STOCK+" WHERE purchase_id = ? ",new String[]{id});
    }

    public Cursor getStockRack(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT * FROM "+ OdooData.TABLE_RACK+"",null);
    }


    public Cursor fillStockitemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT name, product_qty, price_unit, id , product_id, revision_qty, issues, product_description, product_relabel FROM "+ OdooData.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+subId+"'",null);
    }

    public int changeStockState(String id){
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("ID", id);
        ContentValues values = new ContentValues();
        values.put("state","done");
        int result = db.update(OdooData.TABLE_ORDER,values,"id="+id,null);
        Log.d("Inserted",Integer.toString(result));
        return result;
    }

    /**
     * Get boxes that have same id and routeName, should only return one box
     * @return cursor with matching request
     */
    public Cursor getStockBox(String barcode, String routeName){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id FROM "+ OdooData.TABLE_STOCK_BOX+" WHERE barcode = '"+barcode+"' AND route = '"+routeName+"'",null);
    }

    public Cursor fillProductsListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, display_name FROM "+ OdooData.TABLE_PRODUCT_PRODUCT, null);
        return cursor;
    }

    public Cursor getInventory(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT location_id, product_code, theoretical_qty, id, product_name, product_name, product_description FROM "+ OdooData.TABLE_INVENTORY_LINE+" WHERE is_scanned = 'false' ORDER BY location_id ASC",null);
        return cursor;
    }

    public Cursor checkCountLocation(String loc){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT location_id FROM "+ OdooData.TABLE_INVENTORY_LINE +" WHERE location_id = ?", new String[]{loc});
        return cursor;
    }

    public Cursor checkCountProduct(String productCode, String loc){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM "+ OdooData.TABLE_INVENTORY_LINE +" WHERE product_name = ? AND location_id = ?", new String[]{productCode, loc});
        return cursor;
    }

    public Cursor lookForUser(String user){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, login, name, display_name, in_group_56, in_group_57, in_group_58, in_group_59, in_group_60 FROM "+ OdooData.TABLE_RES_USERS+" WHERE login = '"+user+"'", null);
        return cursor;
    }

    public int setDateToIssues(String id, String date){
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("ID", date);
        ContentValues values = new ContentValues();
        values.put(OdooData.FIELD_SYNC, date);
        int result = db.update(OdooData.TABLE_STOCK_ITEMS,values,"picking_id="+id,null);
        Log.d("Inserted",Integer.toString(result));
        return result;
    }

    /*
     * This ones return mostly checks to see if a certain operation has been completed
     */

    /**
     * Checks if the whole data on the table has been deleted
     * @param tableName  Table where you want data to be removed
     * @return True if the information was deleted
     */
    public boolean deleteDataOnTable(String tableName){
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(tableName,null,null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    /**
     * Deletes rows from a table based on a provided field and the value of said field
     * @param tableName Table where you want data to be removed
     * @param field Field to use
     * @param value Data of the field
     * @return True if rows where deleted
     */
    public boolean deleteDataOnTableFromField(String tableName, String field, String value){
        try {
            Log.d("Deleting...", "In "+tableName+" with field "+field+" and value " + value);
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(tableName, field+" = '"+value+"'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean saveIssuesToDB(String issues, int userId, int id){
        //Check if record is created
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT android_id FROM "+ OdooData.TABLE_ISSUES_LIST+" WHERE picking_id = "+id, null);
        if(cursor.getCount() <= 0){
            //Sort out issues
            ContentValues insertValues = new ContentValues();
            insertValues.put("user_id",userId);
            insertValues.put("issues",issues);
            Log.d("Issues Saved", issues);
            insertValues.put("picking_id",id);
            //Create record
            db.insert(OdooData.TABLE_ISSUES_LIST,null, insertValues);
            return false;
        }else{
            //Update record
            ContentValues updateValues = new ContentValues();
            updateValues.put("issues",issues);
            Log.d("Issues Saved", issues);
            db.update(OdooData.TABLE_ISSUES_LIST,updateValues,"picking_id ="+id,null);

        }
        return true;
    }

}
