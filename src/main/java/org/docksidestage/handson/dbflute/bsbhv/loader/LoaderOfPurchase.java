package org.docksidestage.handson.dbflute.bsbhv.loader;

import java.util.List;

import org.dbflute.bhv.*;
import org.dbflute.bhv.referrer.*;
import org.docksidestage.handson.dbflute.exbhv.*;
import org.docksidestage.handson.dbflute.exentity.*;
import org.docksidestage.handson.dbflute.cbean.*;

/**
 * The referrer loader of purchase as TABLE.
 * @author DBFlute(AutoGenerator)
 */
public class LoaderOfPurchase {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected List<Purchase> _selectedList;
    protected BehaviorSelector _selector;
    protected PurchaseBhv _myBhv; // lazy-loaded

    // ===================================================================================
    //                                                                   Ready for Loading
    //                                                                   =================
    public LoaderOfPurchase ready(List<Purchase> selectedList, BehaviorSelector selector)
    { _selectedList = selectedList; _selector = selector; return this; }

    protected PurchaseBhv myBhv()
    { if (_myBhv != null) { return _myBhv; } else { _myBhv = _selector.select(PurchaseBhv.class); return _myBhv; } }

    // ===================================================================================
    //                                                                       Load Referrer
    //                                                                       =============
    protected List<PurchasePayment> _referrerPurchasePayment;

    /**
     * Load referrer of purchasePaymentList by the set-upper of referrer. <br>
     * purchase_payment by PURCHASE_ID, named 'purchasePaymentList'.
     * <pre>
     * <span style="color: #0000C0">purchaseBhv</span>.<span style="color: #994747">load</span>(<span style="color: #553000">purchaseList</span>, <span style="color: #553000">purchaseLoader</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #553000">purchaseLoader</span>.<span style="color: #CC4747">loadPurchasePayment</span>(<span style="color: #553000">paymentCB</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">paymentCB</span>.setupSelect...
     *         <span style="color: #553000">paymentCB</span>.query().set...
     *         <span style="color: #553000">paymentCB</span>.query().addOrderBy...
     *     }); <span style="color: #3F7E5E">// you can load nested referrer from here</span>
     *     <span style="color: #3F7E5E">//}).withNestedReferrer(<span style="color: #553000">paymentLoader</span> -&gt; {</span>
     *     <span style="color: #3F7E5E">//    paymentLoader.load...</span>
     *     <span style="color: #3F7E5E">//});</span>
     * });
     * for (Purchase purchase : <span style="color: #553000">purchaseList</span>) {
     *     ... = purchase.<span style="color: #CC4747">getPurchasePaymentList()</span>;
     * }
     * </pre>
     * About internal policy, the value of primary key (and others too) is treated as case-insensitive. <br>
     * The condition-bean, which the set-upper provides, has settings before callback as follows:
     * <pre>
     * cb.query().setPurchaseId_InScope(pkList);
     * cb.query().addOrderBy_PurchaseId_Asc();
     * </pre>
     * @param refCBLambda The callback to set up referrer condition-bean for loading referrer. (NotNull)
     * @return The callback interface which you can load nested referrer by calling withNestedReferrer(). (NotNull)
     */
    public NestedReferrerLoaderGateway<LoaderOfPurchasePayment> loadPurchasePayment(ReferrerConditionSetupper<PurchasePaymentCB> refCBLambda) {
        myBhv().loadPurchasePayment(_selectedList, refCBLambda).withNestedReferrer(refLs -> _referrerPurchasePayment = refLs);
        return hd -> hd.handle(new LoaderOfPurchasePayment().ready(_referrerPurchasePayment, _selector));
    }

    // ===================================================================================
    //                                                                    Pull out Foreign
    //                                                                    ================
    protected LoaderOfMember _foreignMemberLoader;
    public LoaderOfMember pulloutMember() {
        if (_foreignMemberLoader == null)
        { _foreignMemberLoader = new LoaderOfMember().ready(myBhv().pulloutMember(_selectedList), _selector); }
        return _foreignMemberLoader;
    }

    protected LoaderOfProduct _foreignProductLoader;
    public LoaderOfProduct pulloutProduct() {
        if (_foreignProductLoader == null)
        { _foreignProductLoader = new LoaderOfProduct().ready(myBhv().pulloutProduct(_selectedList), _selector); }
        return _foreignProductLoader;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Purchase> getSelectedList() { return _selectedList; }
    public BehaviorSelector getSelector() { return _selector; }
}
