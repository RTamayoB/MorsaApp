package com.example.morsaapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import static java.util.Arrays.asList;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class OdooConn {

    //Declaration of variables
    private String db, user, pass, url_common, url_object;
    private int uid;
    private XmlRpcClientConfigImpl common;
    private XmlRpcClient models;

    public int getUid() {
        return uid;
    }

    public OdooConn(String user, String pass, Context context) throws MalformedURLException {

        SharedPreferences serverPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        //Check if data changed
        Log.d("Server Data", serverPrefs.getString("url","URL")+"-"+serverPrefs.getString("db","DB"));

        this.db = serverPrefs.getString("db","DB");
        //this.db = "MORSA";
        this.user = user;
        this.pass = pass;
        String url = serverPrefs.getString("url","URL");
        //String url = "https://test.morsa.exinnotch.com";
        this.url_common = url +"/xmlrpc/2/common";
        this.url_object = url +"/xmlrpc/2/object";

        //Create instance of XmlRpcClientConfigImpl and set the common url
        this.common = new XmlRpcClientConfigImpl();
        common.setServerURL(new URL(url_common));
        //Create instance of the XmlRpcClient and set the object url
        this.models = new XmlRpcClient() {{
            setConfig(new XmlRpcClientConfigImpl() {{
                setServerURL(new URL(url_object));
            }});
        }};
    }

    //Get authentication to be able to access the method calls
    public boolean  authenticateOdoo() throws XmlRpcException
    {
        //Get uid to interact with database
        try{
            uid = (int)models.execute(common, "authenticate", asList(
                    db, user, pass, emptyMap()));
            Log.d("Success",Integer.toString(uid));
            return true;
        }catch (ClassCastException classCast){
            Log.d("Bad uid", "Not able to get uid, check that db information is correct");
            return false;
        }
    }

    public String movesTest(String location, Integer pickingId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move", "search",
                asList(asList(
//                        asList("location_dest_id.name","=", location),
                        asList("picking_originative_id", "=", pickingId)
                )),
                emptyMap()
        )));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list.toString();
    }

    public String searchProduct(String barcode) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "product.product", "search",
                asList(asList(
                        "|",
                        asList("alternative_barcode_1", "=",barcode),
                        "|",
                        asList("alternative_barcode_2", "=",barcode),
                        "|",
                        asList("alternative_barcode_3", "=",barcode),
                        "|",
                        asList("barcode", "=", barcode),
                        "|",
                        asList("hs_code","=",barcode),
                        asList("partner_barcode","=",barcode)
                )),
                emptyMap()

        )));
        return list.toString();
    }

    public List searchProductById(Integer productId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "product.product", "search_read",
                asList(asList(
                        asList("id", "=", productId)
                )),
                //emptyMap
                new HashMap() {{
                    put("fields",asList("partner_barcode","barcode","hs_code"));
                }}
        )));
        return list;
    }

    public String searchProductName(String barcode) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "product.product", "search_read",
                asList(asList(
                        "|",
                        asList("barcode", "=", barcode),
                        "|",
                        asList("hs_code","=",barcode),
                        asList("partner_barcode","=",barcode)
                )),
                new HashMap() {{
                    put("fields", asList("name"));
                }}
        )));
        return list.toString();
    }


    public String reStock(String username, Integer userId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move", "search_read",
                asList(asList(
                        asList("user_id", "=", userId),
                        asList("is_completed","=", false),
                        asList("location_id.is_leftover","=", true),
                        asList("state","!=","cancel")
                )),
                emptyMap()
        )));

        Log.d("RESTOCK",list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return returned;
    }

    public String testReStock(Integer userId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking", "search",
                asList(asList(
                        asList("location_dest_id.is_rack", "=", true),
                        //asList("sale_id","=", false),
                        asList("user_id","=",userId)
                )),
                emptyMap()
        )));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list.toString();
    }

    public List<Object> setMovesQty(Integer moveId, Integer moveQty) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move", "update_move_qty", //Before was update_move_qty
                asList(
                        moveId,
                        moveQty
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list;
    }

    public List<String> sendToCart(Integer moveId, Integer moveQty) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move", "update_move_qty_transfer",
                asList(
                        moveId,
                        moveQty
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list;
    }

    public List<String> countPicking(Integer moveId, Integer moveQty) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move", "count_picking",
                asList(
                        moveId,
                        moveQty
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list;
    }

    public List<String> notifyMissing(Integer stockRackId,HashMap<Integer, Integer> map) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.rack", "notify_missing",
                asList(
                        stockRackId,
                        map
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);

        return list;
    }

    public List<String> getPdf(String sessionId, String routeId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.package.route", "action_move_to_route",
                asList(
                        routeId,
                        sessionId
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);
        Log.d("List", list.toString());
        return list;
    }

    public List<String> scanBox(String boxId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.box", "scan_box",
                asList(
                        boxId
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);
        Log.d("List", list.toString());
        return list;
    }

    public List<String> sendPlates(String routeId, String plates) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.package.route", "action_set_plates",
                asList(
                        routeId,
                        plates
                )
                )
        ));


//        Gson gson = new Gson();
//        String returned = gson.toJson(list);
        Log.d("List", list.toString());
        return list;
    }

    //-------------------------
    //Functions to get tables

    public String getPurchaseOrder(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "purchase.order","search_read",
                asList(asList(
                        asList("state","=","purchase"),
                        asList("id", "not in", idList))),
                new HashMap(){{
                    put("fields", asList("id", "display_name", "name", "date_order", "group_id", "partner_id", "amount_total", "partner_id_street", "partner_ref", "state"));
                }}
        )));
        Log.d("PURCHASE ORDER", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getPurchaseOrderLine(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "purchase.order.line","search_read",
                asList(asList(
                        asList("id", "not in", idList))),
                new HashMap() {{
                    put("fields", asList("id","display_name", "product_qty", "price_unit", "price_total", "order_id"));
                }}
        )));
        Log.d("PURCHASE ORDER LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockPicking() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","search_read",
                emptyList(),
                new HashMap() {{
                    put("fields", asList("id", "name", "group_id", "partner_id", "state", "issues_set", "date_done", "purchase_id", "sequence", "origin_invoice_purchase","move_arrangement_ids"));
                }}
        )));
        Log.d("STOCK PICKING", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Gives me all stock boxes
     * @return
     * @throws XmlRpcException
     */
    public String getStockBoxes() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.box","search_read",
                emptyList(),
                new HashMap() {{
                    put("fields", asList("id", "route","invoices","box","scan_box"));
                }}
        )));
        Log.d("STOCK PICKING", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Returns all boxes that match the route
     * @return
     * @throws XmlRpcException
     */
    public String   getRouteBoxes(String route) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.box","search_read",
                asList(asList(
                        "|",
                        asList("package_id.invoice_ids","!=",false),
                        asList("package_id.is_order_transfer","=",true),
                        asList("route","=",route),
                        asList("is_scanned","=", false)
                )),
                new HashMap() {{
                    put("fields", asList("id", "route","invoices","box","barcode","is_scanned"));
                }}
        )));
        Log.d("STOCK BOX", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Get pickings by id for the rack
     */
    public String getPickingFromRack(String id) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","search_read",
                asList(asList(
                        asList("id","=",id)
                )),
                new HashMap() {{
                    put("fields", asList("id", "name", "group_id", "partner_id", "state", "issues_set", "date_done", "purchase_id", "sequence", "origin_invoice_purchase","move_arrangement_ids"));
                }}
        )));
        Log.d("PICKINGS FROM RACK", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Only returns stock.picking for location (where it has move arrangement ids)
     */
    public String getLocations() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.arrangement","search_read",
                asList(),
                new HashMap() {{
                    put("fields", asList("id", "supplier_id", "name", "folio", "num_products"));
                }}
        )));
        int size = list.size();
        Log.d("LOCATIONS QTY", Integer.toString(size));
        Log.d("STOCK ARR LOCATIONS", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Only returns stock.picking for inspection (where state = done)
     * @return
     * @throws XmlRpcException
     */
    public String getInspections() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","search_read",
                asList(asList(
                        asList("move_arrangement_ids","=",false),
                        asList("picking_type_code", "=", "incoming"),
                        asList("state","=","done"),
                        asList("group_id","!=",false),
                        "|",
                        asList("origin_invoice_purchase","!=",""),
                        asList("return_id","!=",false)
                )),
                new HashMap() {{
                    put("fields", asList("id", "name", "group_id", "partner_id", "state", "issues_set", "date_done", "purchase_id", "sequence", "origin_invoice_purchase","return_id", "origin"));
                }}
        )));
        Log.d("STOCK PICKING", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getInspectionsByList(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","search_read",
                asList(asList(
                        asList("id","not in",idList),
                        asList("move_arrangement_ids","=",false),
                        asList("picking_type_code", "=", "incoming"),
                        asList("state","=","done"),
                        asList("group_id","!=",false),
                        "|",
                        asList("origin_invoice_purchase","!=",""),
                        asList("return_id","!=",false)
                )),
                new HashMap() {{
                    put("fields", asList("id", "name", "group_id", "partner_id", "state", "issues_set", "date_done", "purchase_id", "sequence", "origin_invoice_purchase","return_id", "origin"));
                }}
        )));
        Log.d("STOCK PICKING", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockMove(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move","search_read",
                asList(asList(
                        asList("id", "not in", idList))),
                new HashMap() {{
                    put("fields", asList("product_id", "remaining_qty", "total_qty", "location_dest_id", "quantity_done", "product_id", "id", "picking_id", "name", "price_unit", "product_qty", "state", "location_id", "is_completed", "picking_originative_id"));
                }}
        )));
        Log.d("STOCK MOVE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    /**
     * Get the stock.move lines for the picking
     * @param pickingId the Picking we are looking the moves for
     * @return The list of sotck.move
     * @throws XmlRpcException
     */
    public String getInspectionItems(int pickingId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move","search_read",
                asList(asList(
                        asList("picking_id","=",pickingId)
                )),
                new HashMap() {{
                    put("fields", asList("product_id", "remaining_qty", "total_qty", "location_dest_id", "quantity_done", "product_id", "id", "picking_id", "name", "price_unit", "product_qty", "state", "location_id", "is_completed", "picking_originative_id", "transfer_qty", "product_description", "product_relabel"));
                    HashMap context = new HashMap(){{
                        put("display_default_code", false);
                    }};
                }}
        )));
        Log.d("STOCK MOVE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getLocationsItems(int supplierId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move","search_read",
                asList(asList(
                        asList("picking_originative_id.group_id","!=",false),
                        asList("picking_originative_id","!=",false),
                        asList("state","in",asList("confirmed", "assigned", "partially_available")),
                        asList("picking_originative_id.partner_id","=",supplierId)
                )),
                new HashMap() {{
                    put("fields", asList("product_id", "remaining_qty", "total_qty", "location_dest_id", "quantity_done", "product_id", "id", "picking_id", "name", "price_unit", "product_qty", "state", "location_id", "is_completed", "picking_originative_id"));
                }}
        )));
        Log.d("STOCK MOVE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockMoveIssue() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.move.issue","search_read",
                asList(asList()),
                new HashMap() {{
                    put("fields", asList("id","name","issue","devolution"));
                }}

        )));
        Log.d("STOCK MOVE ISSUE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public List sendExtras(int pickingId, String name, int qty, int type) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","synchronized_mistaken_product",
                asList(
                        pickingId,
                        name,
                        qty,
                        type
                )
        )));
        Log.d("Send Extras", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  list;
    }

    public String getStockLocation() throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.location","search_read",
                asList(asList(
                        asList("usage","=","internal"))),
                new HashMap() {{
                    put("fields", asList("id", "name", "usage"));
                }}
        )));
        Log.d("STOCK LOCATION", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getProduct() throws XmlRpcException
    {
        List<Integer> idList = filList();

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "product.product","search_read",
                asList(asList(
                        asList("id", "in", idList))),
                new HashMap() {{
                    put("fields", asList("id", "name", "display_name", "barcode", "default_code"));
                }}

        )));
        Log.d("PRODUCT PRODUCT", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockPickingMoves(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.picking","search_read",
                asList(asList(
                        asList("id", "not in", idList),
                        asList("partner_id", "=", 1)
//                        asList("partner_id", "=", false)
                        )
                ),
                new HashMap() {{
                    put("fields", asList("id", "name", "group_id", "partner_id", "state", "sequence"));
                }}
        )));
        Log.d("STOCK PICKING 2", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getRoutes() throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "exinno.route","search_read",
                asList(asList()),
                new HashMap(){{
                    put("fields", asList("id", "name"));
                }}
        )));
        Log.d("EXINNO ROUTE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockRack(int userId) throws XmlRpcException
    {
        List<String> states = new ArrayList<>();
        states.add("pending");
        states.add("transfer");
        states.add("missing");
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,

                "stock.rack","search_read",
                asList(asList(
                        //asList("id", "not in", idList),
                        asList("picking_ids.user_id", "=", userId),
                        asList("state", "in", states)
//                        asList("partner_id", "=", false)
                        )
                ),
                new HashMap(){{
                    put("fields", asList("id", "display_name", "name", "done_picking", "picking_ids", "create_date", "order_type", "state"));
                }}

        )));
        Log.d("STOCK RACK", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockInventoryLine(int userId) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.inventory.line","search_read",
                asList(asList(
                        asList("state", "=", "confirm"),
                        asList("user_id", "=", userId),
                        asList("is_scanned", "!=", true)) //Remember to change so doesnt use default x
                ),
                new HashMap() {{
                    put("fields", asList("id", "location_id", "product_code", "theoretical_qty", "create_uid", "user_id","product_id,","product_name","is_scanned", "product_description"));
                }}
        )));
        Log.d("STOCK INVENTORY LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getInvoice(/*List<Integer> idList*/) throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "account.invoice","search_read",
                asList(asList(
                        asList("type", "=", "in_invoice"),
                        asList("purchase_id", "!=", false),
                        asList("state","=","open"),
                        asList("app_confirm","=",false)
                        //asList("id", "not in", idList)
                )),
                //No abra cambios en la app
                new HashMap(){{
                    put("fields", asList("id", "name", "display_name", "number", "datetime_invoice", "partner_id", "amount_total", "origin", "purchase_id", "state"));
                }}
        )));
        Log.d("INVOICE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getStockReturn(/*List<Integer> idList*/) throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.return.user","search_read",
                emptyList(),
                new HashMap(){{
                    put("fields", asList("id", "name", "user_id"));
                }}
        )));
        Log.d("STOCK RETURN", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getInvoiceLine() throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "account.invoice.line","search_read",
                emptyList(),
                new HashMap(){{
                    put("fields", asList("id", "name", "display_name", "product_id", "quantity", "price_unit", "price_subtotal", "invoice_id"));
                }}
        )));
        Log.d("INVOICE LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getResUsers() throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "res.users","search_read",
                emptyList(),
                new HashMap(){{
                    put("fields", asList("id", "login", "name", "display_name", "in_group_56", "in_group_57", "in_group_58", "in_group_59", "in_group_60"));
                }}
        )));
        Log.d("INVOICE LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getPermissions(String user) throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "res.groups","search_read",
                emptyList(),
                emptyMap()
        )));
        /*
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "res.users","search_read",
                asList(asList(
                        asList("login","=",user)
                )),
                new HashMap(){{
                    put("fields", asList("id", "login", "name", "display_name", "in_group_56", "in_group_57", "in_group_58", "in_group_59", "in_group_60"));
                }}
        )));*/
        Log.d("USERS", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String reloadInvoiceLines(int invoice_id) throws XmlRpcException
    {

        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "account.invoice.line","search_read",
                asList(asList(
                        asList("invoice_id","=",invoice_id)
                )),
                new HashMap(){{
                    put("fields", asList("id", "name", "display_name", "product_id", "quantity", "price_unit", "price_subtotal", "invoice_id"));
                }}
        )));
        Log.d("INVOICE LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String reloadStockReturnLines(int return_id) throws XmlRpcException
    {
        List<String> states = new ArrayList<>();
        states.add("draft");
        states.add("inspecting");
        states.add("overdue");
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,
                "stock.return.line","search_read",
                asList(asList(
                        asList("return_id.state","in",states),
                        asList("return_id.user_id","=", return_id)
                )),
                new HashMap(){{
                    put("fields", asList("id", "name", "product_id", "qty", "state", "user_id", "accepted_qty", "rejected_qty"));
                }}
        )));
        Log.d("STOCK RETURN LINE", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public List doRefund(int productId, HashMap<String, Integer> qty) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.return.line","update_inspected_qty",
                        asList(
                                productId,
                                qty
                        )
                        )
                )
        );
        Log.d("STOCK RETURN LINE", list.toString());

        return  list;
    }

    //----------------------------

    public String getPickingIdsRack(List<Integer> idList) throws XmlRpcException
    {
        List<String> states = new ArrayList<>();
        states.add("pending");
        states.add("transfer");
        states.add("missing");
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,uid,pass,

                "stock.rack","search_read",
                asList(asList(
                        asList("id", "not in", idList),
                        asList("picking_ids.user_id", "=", 1),
                        asList("state", "in", states)
//                        asList("partner_id", "=", false)
                        )
                ),
                new HashMap() {{
                    put("fields", asList("id", "picking_ids"));
                }}

        )));
        Log.d("STOCK RACK DATA", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }

    public String getSequence(List<Integer> idList) throws XmlRpcException
    {
        List list = asList((Object[])models.execute("execute_kw", asList(
                db,
                uid,
                pass,
                "stock.move",
                "search_read",
                asList(asList(asList("id", "in", idList))),
                new HashMap() {{ put("fields", asList("id"));put("order","travel_sequence ASC");}}
        )));
        Log.d("RawData", list.toString());
        Gson gson = new Gson();
        String returned = gson.toJson(list);

        return  returned;
    }



    public List confirmInvoice(Integer id, String number) throws XmlRpcException
    {
        List list = asList(
            (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "purchase.order","synchronized_transfer_app",
                        asList(
                                id,
                                number
                        )
                )
            )
        );
        Log.d("RawData", list.toString());

        return  list;
    }

    public List confirmStockReturn(Integer id) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.return","action_received",
                        asList(
                                id
                        )
                        )
                )
        );
        Log.d("RawData", list.toString());

        return  list;
    }

    public List confirmIssues(int stockId, HashMap<Integer, HashMap<String, Object>> issues) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.picking","synchronized_issues",
                        asList(
                                stockId,
                                issues
                        )
                        )
                )
        );
        Log.d("RawData", list.toString());

        return  list;
    }

    public String actionClose(int returnId) throws XmlRpcException
    {
        List list = asList(
                 (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.return","action_close",
                        asList(
                                returnId
                        )
                        )
                )
        );
        Log.d("ActionClose", list.toString());

        return  list.toString();
    }

    public String actionRejected(int returnId) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.return","action_rejected",
                        asList(
                                returnId
                        )
                        )
                )
        );
        Log.d("ActionRejected", list.toString());

        return  list.toString();
    }

    public List sendTransfers(int origin, int destiny, HashMap<Integer, Integer> productsToTransfer) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.picking","internal_transfer",
                        asList(
                                origin,
                                destiny,
                                productsToTransfer
                        )
                        )
                )
        );
        Log.d("RawData", list.toString());

        return  list;
    }

    public String sendCount(HashMap<Integer, Integer> products) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.inventory.line","update_product_qty",
                        asList(
                                products
                        )
                        )
                )
        );
        Log.d("Send Count Result", list.toString());

        return  list.toString();
    }

    public List addCount(String productName, Integer qty) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.inventory.line","create_new_product",
                        asList(
                                productName,
                                qty
                        )
                        )
                )
        );
        Log.d("Send Count Result", list.toString());

        return  list;
    }

    public List<Object> computeTheoreticalQty(Integer id) throws XmlRpcException
    {
        List list = asList(
                (Object[])models.execute("execute_kw", asList(
                        db,uid,pass,
                        "stock.inventory.line","compute_theoretical_qty",
                        asList(
                                id
                        )
                        )
                )
        );
        Log.d("Send Count Result", list.toString());

        return  list;
    }

    public List<Integer> filList(){

        List<Integer> idList = asList(
                8872,
                8876,
                8963,
                8868,
                8935,
                8866,
                8867,
                8899,
                8890,
                8928,
                8895,
                8894,
                8893,
                8892,
                8891,
                8896,
                8913,
                8950,
                8879,
                8962,
                8933,
                8951,
                8907,
                8926,
                8914,
                8884,
                8918,
                8916,
                8915,
                8873,
                8764,
                8880,
                8925,
                8878,
                8958,
                8931,
                8881,
                8930,
                8920,
                8917,
                8934,
                8923,
                8959,
                8871,
                8938,
                8919,
                8885,
                8898,
                8945,
                8952,
                8922,
                8961,
                8897,
                8888,
                8937,
                8912,
                8955,
                8954,
                8953,
                8942,
                8947,
                8908,
                8911,
                8901,
                8870,
                8869,
                8886,
                8887,
                8889,
                8956,
                8936,
                8940,
                8900,
                8902,
                8905,
                8921,
                8924,
                8939,
                8949,
                8941,
                8932,
                8882,
                8903,
                8877,
                8904,
                8929,
                8883,
                8906,
                8964,
                8946,
                8957,
                8960,
                8909,
                8927,
                8948,
                8943,
                8910,
                8944,
                8875,
                8874);

        return idList;
    }
}
