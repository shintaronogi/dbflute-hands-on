package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Resource;

import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

/**
 * @author shiny
 */
public class HandsOn02Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    /**
     * テストデータが存在すること
     */
    public void test_existsTestData() {
        int memberCount = memberBhv.selectCount(cb -> {
            // TODO shiny selectCount()にorder byは意味がないのでなくてOK (要件にもない) by jflute (2025/04/02)
            cb.query().addOrderBy_MemberId_Desc();
        });

        assertTrue(memberCount > 0);
    }

    /**
     * 会員名称がSで始まる会員を検索
     * o 会員名称の昇順で並べる
     * o (検索結果の)会員名称がSで始まっていることをアサート
     * o "該当テストデータなし" や "条件間違い" 素通りgreenにならないように素通り防止を
     */
    public void test_member_name_starts_with_S() {
        // ## Arrange ##
        String prefix = "S";

        // ## Act ##
        // [1on1でのふぉろー] ListResultBeanのお話、DBFluteとしては具象のまま扱って欲しい感がある
        // [1on1でのふぉろー] IntelliJだと、.var で左辺(戻り値)の補完ができる
        // Java10ぐらいからは、varが使えるようになったので、ローカル変数においては補完を使わないこともあるが...
        // 現場での浸透度と現実問題の話。
        List<Member> memberList = memberBhv.selectList(cb -> {
            // TODO shiny 後でどうsqlが生成されてるのかみてみる (2025/04/02)
            // いつか、SQLのフォーマット揃えるプログラムのお話をしたい by jflute (2025/04/02)
            // TODO shiny optionのところは、opって短い慣習的な名前にしているので合わせてもらえればと by jflute (2025/04/02)
            cb.query().setMemberName_LikeSearch(prefix, option -> option.likePrefix());
        });

        // ## Assert ##
        assertFalse(memberList.isEmpty());
        memberList.forEach(member -> {
            // TODO shiny すでに宣言してる memberName を使おう by jflute (2025/04/02)
            // IntelliJだと、control+Tでリファクタメニューが出てきて、そこで変数の抽出を使うと良い
            String memberName = member.getMemberName();
            log("memberName: {}", memberName);
            assertTrue(member.getMemberName().startsWith(prefix));
        });
    }
    // TODO shiny [読み物課題] リファクタリングは思考のツール by jflute (2025/04/02)
    // https://jflute.hatenadiary.jp/entry/20121202/1354442627

    // TODO jflute 時間1on1はここから (2025/04/02)
    /**
     * 会員IDが1の会員を検索
     * o 一件検索として検索すること
     * o 会員IDが 1 であることをアサート
     */
    public void test_member_id_is_1() {
        // ## Arrange ##
        Integer targetId = 1;
        // ## Act ##
        memberBhv.selectByPK(1).alwaysPresent(member -> {
            // ## Assert ##
            Integer memberId = member.getMemberId();
            log("memberId: {}", memberId);
            assertEquals(member.getMemberId(), targetId);
        });
    }

    /**
     * 生年月日がない会員を検索
     * o 更新日時の降順で並べる
     * o 生年月日がないことをアサート
     */
    public void test_member_birthdate_is_null() {
        // ## Arrange ##
        // ## Act ##
        List<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().setBirthdate_IsNull();
            cb.query().addOrderBy_UpdateDatetime_Desc();
        });
        // ## Assert ##
        assertFalse(memberList.isEmpty());
        memberList.forEach(member -> {
            LocalDate birthDate = member.getBirthdate();
            log("memberName: {}, birthDate: {}", member.getMemberName(), birthDate);
            assertNull(birthDate);
        });
    }
}
