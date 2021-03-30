package com.example.morsaapp;

/**
 * Date fields
 * Account invoice - create_date
 * Stock Picking - create_date
 */


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.barteksc.pdfviewer.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DBConnect extends SQLiteOpenHelper {

    public DBConnect(@Nullable Context context, @Nullable String name, @Nullable android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db) {
        db.execSQL(Utilities.CREATE_TABLE_ORDERS);
        db.execSQL(Utilities.CREATE_TABLE_ITEMS);
        db.execSQL(Utilities.CREATE_TABLE_STOCK);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_ITEMS);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_ISSUES);
        db.execSQL(Utilities.CREATE_TABLE_ISSUES_LIST);
        db.execSQL(Utilities.CREATE_TABLE_PRODUCT_PRODUCT);
        db.execSQL(Utilities.CREATE_TABLE_USERS);
        db.execSQL(Utilities.CREATE_TABLE_ROUTES);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_BOX);
        db.execSQL(Utilities.CREATE_TABLE_RACK);
        db.execSQL(Utilities.CREATE_TABLE_INVENTORY_LINE);
        db.execSQL(Utilities.CREATE_TABLE_INVOICE);
        db.execSQL(Utilities.CREATE_TABLE_INVOICE_LINE);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_ARRANGEMENT);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_RETURN);
        db.execSQL(Utilities.CREATE_TABLE_STOCK_RETURN_LINE);
        db.execSQL(Utilities.CREATE_TABLE_RES_USERS);
    }

    @Override
    public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+Utilities.TABLE_ORDER);
        db.execSQL("DROP TABLE IF EXISTS "+Utilities.TABLE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_STOCK);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_STOCK_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_STOCK_ISSUES);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_ISSUES_LIST);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_PRODUCT_PRODUCT);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_ROUTES);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_STOCK_BOX);
        db.execSQL("DROP TABLE IF EXISTS "+ Utilities.TABLE_RACK);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_INVENTORY_LINE);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_INVOICE);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_INVOICE_LINE);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_STOCK_ARRANGEMENT);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_STOCK_RETURN);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_STOCK_RETURN_LINE);
        db.execSQL("DROP TABLE IF EXISTS " + Utilities.TABLE_RES_USERS);
        onCreate(db);
    }

    public boolean sync(String Json, String tableName){
        try {
            JSONArray jsonArray = new JSONArray(Json);
            fillTable(jsonArray,tableName);
        }catch (JSONException json){
            Log.d("Error fill","Error filling Table");
        }
        return true;
    }

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

    public Cursor fillOrdersListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, display_name ,name, date_order, group_id, partner_id, amount_total, partner_id_street, partner_ref FROM "+Utilities.TABLE_ORDER+" WHERE state = 'purchase'",null);
    }

    public Cursor fillInvoiceListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, display_name, number, datetime_invoice, partner_id, amount_total, origin, purchase_id FROM "+Utilities.TABLE_INVOICE+" WHERE state = 'open'",null);
    }

    public Cursor fillRefundListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, type_id, partner_id, date, state, amount_total FROM "+Utilities.TABLE_STOCK_RETURN,null);
    }

    public Cursor getRoutes(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name FROM "+Utilities.TABLE_ROUTES+"",null);
    }

    public Cursor getRoute(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name FROM "+ Utilities.TABLE_ROUTES+" WHERE name = '"+name+"'", null);
    }

    public Cursor fillIncidenciesListView(boolean isDevolution){
        SQLiteDatabase db = this.getReadableDatabase();
        if(isDevolution){
            return db.rawQuery("SELECT * FROM "+Utilities.TABLE_STOCK_ISSUES+" WHERE devolution = 'true'",null);
        }
        else{
            return db.rawQuery("SELECT * FROM "+Utilities.TABLE_STOCK_ISSUES+" WHERE issue = 'true'",null);
        }
    }

    public Cursor fillItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT display_name, product_qty, price_unit, price_total FROM "+Utilities.TABLE_ITEMS+" WHERE order_id ='"+subId+"'",null);
    }

    public Cursor fillInvoiceItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT display_name, quantity, price_unit, price_subtotal FROM "+Utilities.TABLE_INVOICE_LINE+" WHERE invoice_id ='"+subId+"'",null);
    }

    public Cursor fillRefundItemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT name, id , product_id , price_unit , qty, return_id FROM "+Utilities.TABLE_STOCK_RETURN_LINE+" WHERE return_id ='"+subId+"'",null);
    }

    public Cursor fillStockListView(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, name, group_id, partner_id, date_done, origin_invoice_purchase, return_id, origin FROM "+Utilities.TABLE_STOCK+" /*WHERE state = 'done' AND group_id != 'false' AND origin_invoice_purchase != ''*/ ORDER BY origin_invoice_purchase ASC",null); //group_id != false is temporal fix
    }

    public boolean reloadInspections(){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK, "state = 'done'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadInspectionLines(String pickingId){
        try {
            Log.d("PickingId-Move", pickingId);
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK_ITEMS, "picking_id = '"+pickingId+"'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadInvoiceLines(String invoiceId){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_INVOICE_LINE, "invoice_id = '"+invoiceId+"'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadLocations(){
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK,"move_arrangement_ids != '[]'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadLocationLines(String pickingId){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK_ITEMS, null,null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadPickings(String pickingId){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK, "id = "+pickingId,null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public boolean reloadStockBoxes(String routeName){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(Utilities.TABLE_STOCK_BOX, "route = '"+routeName+"'",null);
            return true;
        }catch (Exception e) {
            Log.d("Error", e.toString());
            return false;
        }
    }

    public Cursor getStockBoxesFromRoute(String route){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, route, invoices, box FROM "+Utilities.TABLE_STOCK_BOX+" WHERE route = '"+route+"' AND is_scanned = 'false'",null);
    }

    public Cursor fillPickingsListView(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, name, group_id, partner_id, date_done FROM "+Utilities.TABLE_STOCK+" WHERE id in "+list+"",null);
    }

    public Cursor fillLocationsListView(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id, partner_id, name, folio, num_products FROM "+Utilities.TABLE_STOCK_ARRANGEMENT,null);
    }

    public Cursor movesTest(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE id in "+list,null);
    }

    public Cursor getLocationMoves(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id, product_description FROM "+Utilities.TABLE_STOCK_ITEMS+" /*WHERE picking_originative_id = ?*/ ORDER BY location_dest_id ASC",null);
    }

    public Cursor movesTestReStock(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id, product_description FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE state != 'cancel'",null);
    }

    public Cursor pickingMovesTest(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id, location_id FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+list+"'",null);
    }

    public Cursor pickingMovesById(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, transfer_qty, product_id, id, picking_id, location_id, product_description, name FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE id = '"+list+"'",null);
    }

    public Cursor getPickingMoveIds(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+list+"'",null);
    }


    public Cursor pickingMovesInIds(String list){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT product_id, remaining_qty, total_qty, location_dest_id, quantity_done, product_id, id, picking_id FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE picking_id in "+list+"",null);
    }

    public Cursor getNameFromPicking(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT name FROM "+Utilities.TABLE_STOCK+" WHERE id = '"+id+"'",null);
    }

    public Cursor getStockStatefromOrder(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT state FROM "+Utilities.TABLE_STOCK+" WHERE purchase_id = ? ",new String[]{id});
    }

    public Cursor getStockRack(){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT * FROM "+Utilities.TABLE_RACK+"",null);
    }


    public Cursor fillStockitemsListView(String subId){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT name, product_qty, price_unit, id , product_id, revision_qty, issues FROM "+Utilities.TABLE_STOCK_ITEMS+" WHERE picking_id = '"+subId+"'",null);
    }

    public int changeStockState(String id){
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("ID", id);
        ContentValues values = new ContentValues();
        values.put("state","done");
        int result = db.update(Utilities.TABLE_ORDER,values,"id="+id,null);
        Log.d("Inserted",Integer.toString(result));
        return result;
    }

    /**
     * Get boxes that have same id and routeName, should only return one box
     * @return cursor with matching request
     */
    public Cursor getStockBox(String barcode, String routeName){
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT id FROM "+Utilities.TABLE_STOCK_BOX+" WHERE barcode = '"+barcode+"' AND route = '"+routeName+"'",null);
    }

    public Cursor fillProductsListView(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, display_name FROM "+Utilities.TABLE_PRODUCT_PRODUCT, null);
        return cursor;
    }

    public Cursor getInventory(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT location_id, product_code, theoretical_qty, id, product_name, product_name, product_description FROM "+Utilities.TABLE_INVENTORY_LINE+" WHERE is_scanned = 'false' ORDER BY location_id ASC",null);
        return cursor;
    }

    public Cursor checkCountLocation(String loc){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT location_id FROM "+Utilities.TABLE_INVENTORY_LINE +" WHERE location_id = ?", new String[]{loc});
        return cursor;
    }

    public Cursor checkCountProduct(String productCode, String loc){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_INVENTORY_LINE +" WHERE product_name = ? AND location_id = ?", new String[]{productCode, loc});
        return cursor;
    }

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

    public Cursor lookForUser(String user){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, login, name, display_name, in_group_56, in_group_57, in_group_58, in_group_59, in_group_60 FROM "+Utilities.TABLE_RES_USERS+" WHERE login = '"+user+"'", null);
        return cursor;
    }

    public int setDateToIssues(String id, String date){
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("ID", date);
        ContentValues values = new ContentValues();
        values.put(Utilities.FIELD_SYNC, date);
        int result = db.update(Utilities.TABLE_STOCK_ITEMS,values,"picking_id="+id,null);
        Log.d("Inserted",Integer.toString(result));
        return result;
    }

    //Might delete
//    public boolean reSyncTable(String jsonData, String tableName) throws JSONException{
//        SQLiteDatabase db = this.getWritableDatabase();
//        JSONArray data = new JSONArray(jsonData);
//        for (int i = 0; i < data.length(); i++) {
//            JSONObject jsonObject = data.getJSONObject(i);
//            int intId = (int)jsonObject.get("id");
//            String currentId = Integer.toString(intId);
//            Cursor cursor = db.rawQuery("SELECT id FROM "+tableName, null);
//            while(cursor.moveToNext()){
//                if(cursor.getString(cursor.getColumnIndex("id")).equals(currentId)){
//                    Iterator<String> keys = jsonObject.keys();
//                    while (keys.hasNext()){
//                        String key = keys.next();
//                        String value = jsonObject.get(key).toString();
//                        ContentValues values = new ContentValues();
//                        values.put(key, value);
//                        db.update(tableName,values,"id="+currentId,null);
//                    }
//                }else{
//                    Iterator<String> keysInsert = jsonObject.keys();
//                    ContentValues valuesInsert = new ContentValues();
//                    while (keysInsert.hasNext()){
//                        String key = keysInsert.next();
//                        String value = jsonObject.get(key).toString();
//                        valuesInsert.put(key, value);
//                    }
//                    db.insert(tableName,null,valuesInsert);
//                }
//            }
//            cursor.close();
//        }
//
//        return true;
//    }

    public boolean saveIssuesToDB(String issues, int userId, int id){
        //Check if record is created
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT android_id FROM "+Utilities.TABLE_ISSUES_LIST+" WHERE picking_id = "+id, null);
        if(cursor.getCount() <= 0){
            //Sort out issues
            ContentValues insertValues = new ContentValues();
            insertValues.put("user_id",userId);
            insertValues.put("issues",issues);
            Log.d("Issues Saved", issues);
            insertValues.put("picking_id",id);
            //Create record
            db.insert(Utilities.TABLE_ISSUES_LIST,null, insertValues);
            return false;
        }else{
            //Update record
            ContentValues updateValues = new ContentValues();
            updateValues.put("issues",issues);
            Log.d("Issues Saved", issues);
            db.update(Utilities.TABLE_ISSUES_LIST,updateValues,"picking_id ="+id,null);

        }
        return true;
    }

}
