package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.MemberSecurity;
import org.docksidestage.handson.dbflute.exentity.MemberStatus;
import org.docksidestage.handson.dbflute.exentity.Product;
import org.docksidestage.handson.dbflute.exentity.Purchase;
import org.docksidestage.handson.unit.UnitContainerTestCase;

// TODO shiny javadoc by jflute (2025/04/16)
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    @Resource
    private PurchaseBhv purchaseBhv;

    /**
     * 会員名称がSで始まる1968年1月1日以前に生まれた会員を検索
     * 会員ステータスも取得する
     * 生年月日の昇順で並べる
     * 会員が1968/01/01以前であることをアサート
     */
    public void test_1() {
        // ## Arrange ##
        LocalDate targetDate = LocalDate.of(1968, 1, 1);

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberName_LikeSearch("S", op -> op.likePrefix());
            cb.query().setBirthdate_LessEqual(targetDate);
            cb.query().addOrderBy_Birthdate_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        memberList.forEach(member -> {
            // TODO shiny ここでは、カラム名もBIRTHDATEだし、変数名も単に birthdate でもいいかなと by jflute (2025/04/16)
            // 変数のスコープの広さ次第で変数名をどれだけ修飾するか決まる。
            // but ここではカラム名を省略するわけではなく、テーブル名 prefix を外すだけ
            // 名前は識別するためのもの、識別する人のことを想像して判断する
            LocalDate memberBirthdate = member.getBirthdate();
            log(member.getMemberName(), memberBirthdate, member.getMemberStatus());
            assertTrue(memberBirthdate.isEqual(targetDate) || memberBirthdate.isBefore(targetDate));
            // 別のやり方: これが正解というわけではないが参考として
            //assertFalse(memberBirthdate.isAfter(targetDate));
        });
    }

    /**
     * 会員ステータスと会員セキュリティ情報も取得して会員を検索
     * 若い順で並べる。生年月日がない人は会員IDの昇順で並ぶようにする
     * 会員ステータスと会員セキュリティ情報が存在することをアサート
     */
    public void test_2() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.setupSelect_MemberSecurityAsOne();
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        memberList.forEach(member -> {
            // [1on1でのふぉろー]
            // MemberStatusが100%存在する理由は？ => (完璧な回答)
            // (NotNullのFKカラムで参照しているから、探しに行けば絶対にする: 物理的に保証されている)
            assertTrue(member.getMemberStatus().isPresent());

            log(member);

            // [1on1でのふぉろー]
            // MemberSecurityが100%存在する理由は？ => (いっぱい考えた)
            // (カーディナリティのセカンドレベル、ERDだと黒丸、テーブルコメントにも書いてある)
            assertTrue(member.getMemberSecurityAsOne().isPresent());

            // TODO shiny [読み物課題] これ最重要 by jflute (2025/04/16)
            // 会員から会員ステータスは、NotNullのFKカラムで参照しているので、探しにいけば必ず存在する
            // 会員から会員セキュリティは、FKの方向と探しにいく方向が逆なので同じ理論にはなりませんが、
            // ERDのリレーションシップ線に注目。会員退会情報と比べると一目瞭然、黒丸がついていないので必ず存在する1
            //   会員から会員セキュリティ => 1:必ず1 (1:1)
            //   会員から会員退会情報    => 1:いないかもしれない1 (1:0..1)
            // ただ、物理的な制約はありません。業務的というのは、そういうルールにしているいうことだけなんですね。
            // 細かいですが、これがデータベースプログラミングにおいて、とても重要なんですよね。
            // ぜひ、カージナリティに着目してみてください。

            // [1on1でのふぉろー]
            // FK制約を物理的に貼るか貼らないか話、jfluteの意見を説明。
        });
    }

    /**
     * 会員セキュリティ情報のリマインダ質問で2という文字が含まれている会員を検索
     * 会員セキュリティ情報のデータ自体は要らない
     * (Actでの検索は本番でも実行されることを想定し、テスト都合でパフォーマンス劣化させないこと)
     * リマインダ質問に2が含まれていることをアサート
     * アサートするために別途検索処理を入れても誰も文句は言わない
     */
    public void test_3() {
        // ## Arrange ##
        String target = "2";
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch(target, op -> op.likeContain());
        });

        // ## Assert ##
        assertHasAnyElement(memberList);

        // 色々なやり方がありそう。脳の記憶をリフレッシュする為に、久々にStream APIを使ってみる。
        // List<Integer> memberIds = memberList.stream().map(member -> member.getMemberId()).collect(Collectors.toList());

        // Extractという便利なメソッドがあることに気づいた！
        Set<Integer> memberIds = memberList.extractColumnSet(member -> member.getMemberId());
        // これもできそう：memberBhv.extractMemberIdList(memberList);
        ListResultBean<MemberSecurity> memberSecurities = memberSecurityBhv.selectList(cb -> {
            // done jflute "IN" Operatorについて聞きたいです！
            // [回答] orScopeQuery()で同じカラムの等値条件と変わらないけど...
            // MySQLに対してわかりやすく示すという意味では、INの方が良いことがあるかも？
            // (少なくとも、そういう姿勢で実装の判断をしていった方が良い)
            //
            // ちなみに、in, in句、SQLの条件のinとか会話で聞き直すこと一杯。だからInScope。
            //
            // ローマ字のカラム名の歴史、時々シンプルな言葉でローマ字にするのは悪くないと思う。
            cb.query().setMemberId_InScope(memberIds);
        });
        memberList.forEach(member -> {
            Integer memberId = member.getMemberId();
            Optional<String> reminderQuestion = memberSecurities.stream()
                    .filter(security -> security.getMemberId().equals(memberId))
                    .findFirst()
                    .map(security -> security.getReminderQuestion());
            // Stream APIで検索は直感的に分かりずらい
            assertTrue(reminderQuestion.isPresent());
            assertTrue(reminderQuestion.get().contains(target));
            // TODO shiny ログ出すなら、assertよりも前の方が落ちた時に見れる (かつ、optionalのまま出してOK) by jflute (2025/04/23)
            log("Name: {}, ReminderQuestion: {}", member.getMemberName(), reminderQuestion.get());
        });
    }

    /**
     * 会員ステータスの表示順カラムで会員を並べて検索
     * 会員ステータスの "表示順" カラムの昇順で並べる
     * 会員ステータスのデータ自体は要らない
     * その次には、会員の会員IDの降順で並べる
     * 会員ステータスのデータが取れていないことをアサート
     * 会員が会員ステータスごとに固まって並んでいることをアサート (順序は問わない)
     */
    public void test_4() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);

        // ここも色々なやり方ありそう
        // やりやすいアサートとやりにくいやつがある
        // Lambdaだと並んでることをアサートみないなのはしにくい気がする

        // TODO shiny addOrderBy_DisplayOrder_Asc() を外して実行しても落ちない by jflute (2025/04/23)
        Set<String> memberStatusCodeSet = new HashSet<>();
        String previousStatusCode = "";
        for (Member member : memberList) {
            // 会員ステータスがとれていないことのアサート
            assertFalse(member.getMemberStatus().isPresent());

            String currentStatusCode = member.getMemberStatusCode();
            if (currentStatusCode.equals(previousStatusCode)) { // 同じだったら (A, A, A...)
                assertFalse(memberStatusCodeSet.contains(currentStatusCode));
                memberStatusCodeSet.add(currentStatusCode);
            }
        }
    }

    // [1on1でのふぉろー] 基点テーブル
    // 結果セットにPKを付けるとしたら？

    // [1on1でのふぉろー] 要件を絶対間違えないプログラマー
    // 日本語の文章の構造を分析して解釈する習慣
    
    // TODO jflute 次回、サロゲートキーのお話 (2025/04/23)
    // https://dbflute.seasar.org/ja/manual/topic/dbdesign/surrogatekey.html
    /**
     *生年月日が存在する会員の購入を検索
     * 会員名称と会員ステータス名称と商品名を取得する(ログ出力)
     * 購入日時の降順、購入価格の降順、商品IDの昇順、会員IDの昇順で並べる
     * OrderBy がたくさん追加されていることをログで目視確認すること
     * 購入に紐づく会員の生年月日が存在することをアサート
     */
    public void test_5() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Purchase> purchases = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Product();
            // TODO shiny "生年月日が存在する" by jflute (2025/04/23)
            cb.query().addOrderBy_PurchaseDatetime_Desc();
            cb.query().addOrderBy_PurchasePrice_Desc();
            cb.query().addOrderBy_ProductId_Asc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        assertHasAnyElement(purchases);
        purchases.forEach(purchase -> {
            Member member = purchase.getMember().orElseThrow();
            MemberStatus status = member.getMemberStatus().orElseThrow();
            // TODO shiny unusedになってる by jflute (2025/04/23)
            Product product = purchase.getProduct().orElseThrow();

            log("MemberName: {}, MemberStatus: {}, ProductName: {}", member.getMemberName(), status.getMemberStatusName(),
                    purchase.getProductId());

            // 落ちる？
            // log("birthdate: ", member.getBirthdate());
            assertNotNull(member.getBirthdate());
        });
    }
}
