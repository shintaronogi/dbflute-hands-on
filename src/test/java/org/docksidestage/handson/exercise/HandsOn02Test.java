package org.docksidestage.handson.exercise;

import javax.annotation.Resource;

import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author shiny
 */
public class HandsOn02Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    public void test_existsTestData() {
        int memberCount = memberBhv.selectCount(cb -> {
            cb.query().addOrderBy_MemberId_Desc();
        });

        assertTrue(memberCount > 0);
    }
}
