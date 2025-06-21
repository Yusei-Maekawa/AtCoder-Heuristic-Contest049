import java.io.BufferedReader; // 入力を効率的に読み込むためのクラス
import java.io.IOException; // 入出力処理における例外を扱うためのクラス
import java.io.InputStreamReader; // バイトストリームを文字ストリームに変換するためのクラス
import java.util.ArrayList; // 可変長リストの実装
import java.util.HashMap; // キーと値のペアを格納するハッシュマップの実装
import java.util.HashSet; // 重複を許さない要素の集合を格納するハッシュセットの実装
import java.util.List; // リストインターフェース
import java.util.Map; // マップインターフェース
import java.util.Set; // セットインターフェース
import java.util.StringTokenizer; // 文字列を区切り文字で分割するためのクラス

/**
 * メインクラス。
 * プログラムのエントリポイントであり、入力の読み込みとSolverの実行を行います。
 */
public class Main {
    /**
     * プログラムのメインメソッド。
     * @param args 
     * @throws IOException 
     * 入力の読み込み中に発生する可能性のある例外を処理します。
     */

    public static void main(String[] args) throws IOException {
        // 高速な入力処理のためにBufferedReaderを使用します。
        // 標準入力System.inをInputStreamReaderで文字ストリームに変換し、BufferedReaderでバッファリングします。
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        int N = 20; //固定20
        br.readLine(); // 問題の入力フォーマットでは、最初の行にNの値がありますが、今回は固定値なので読み飛ばします。

        // グリッド上の各位置にある箱の重さを格納する2次元配列
        int[][] weights = new int[N][N];
        // グリッド上の各位置にある箱の耐久力を格納する2次元配列
        int[][] durabilities = new int[N][N];

        // 箱の重さデータを読み込みます。
        // N行N列のグリッドに対応する重さの値を順に解析し、weights配列に格納します。
        for (int i = 0; i < N; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine()); // 1行分のデータを空白で分割
            for (int j = 0; j < N; j++) {
                weights[i][j] = Integer.parseInt(st.nextToken()); // 文字列を整数に変換して格納
            }
        }
        // 箱の耐久力データを読み込みます。
        // N行N列のグリッドに対応する耐久力の値を順に解析し、durabilities配列に格納します。
        for (int i = 0; i < N; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine()); // 1行分のデータを空白で分割
            for (int j = 0; j < N; j++) {
                durabilities[i][j] = Integer.parseInt(st.nextToken()); // 文字列を整数に変換して格納
            }
        }

        // Solverクラスのインスタンスを生成し、読み込んだN、weights、durabilitiesのデータを与えます。
        Solver solver = new Solver(N, weights, durabilities);
        // Solverのsolveメソッドを呼び出し、箱の運搬問題を解きます。
        solver.solve();
    }
}

/**
 * ダンボール箱の情報をカプセル化するクラス。
 * 各箱は一意のID、グリッド上の位置、重さ、そして初期耐久力と現在の耐久力を持っています。
 */
class Box {
    int id;           // 箱のユニークな識別子 (0から始まる連番)
    int r, c;         // 箱が現在位置するグリッドの行座標 (r) と列座標 (c)
    int w;            // 箱自体の重さ
    long d_initial;   // 箱の初期耐久力。この値は変化しません。
    long d_current;   // 箱の現在の耐久力。移動によって受けるダメージでこの値が減少します。

    /**
     * Boxクラスのコンストラクタ。
     * 新しい箱オブジェクトを初期化します。
     * @param id 箱のID
     * @param r 箱の行座標
     * @param c 箱の列座標
     * @param w 箱の重さ
     * @param d 箱の初期耐久力
     */
    public Box(int id, int r, int c, int w, int d) {
        this.id = id;
        this.r = r;
        this.c = c;
        this.w = w;
        this.d_initial = d;
        this.d_current = d; // 最初は現在の耐久力も初期耐久力と同じ値で設定されます。
    }

    /**
     * コピーコンストラクタ。
     * 既存のBoxオブジェクトの全てのプロパティを新しいBoxオブジェクトにコピーします。
     * これにより、シミュレーションなどで箱の状態を一時的に変更したい場合に、
     * 元の箱のデータに影響を与えずに安全に操作できます（ディープコピー）。
     * @param other コピー元のBoxオブジェクト
     */
    public Box(Box other) {
        this.id = other.id;
        this.r = other.r;
        this.c = other.c;
        this.w = other.w;
        this.d_initial = other.d_initial;
        this.d_current = other.d_current;
    }
}

/**
 * ダンボール運搬問題の解決ロジックを実装する主要なクラス。
 * 高橋社長の移動、箱のピックアップ、最適な輸送計画の策定、およびその計画のシミュレーションを行います。
 */
class Solver {
    final int N;             // グリッドの一辺のサイズ (例: 20x20グリッドの場合、N=20)
    int px, py;              // 高橋社長の現在位置の行座標 (px) と列座標 (py)
    List<Box> hand;          // 高橋社長が現在手に持っている箱のリスト。スタックのように機能します
                             // (hand.get(hand.size()-1) が一番上に積まれた箱)。
    StringBuilder actions;   // 高橋社長の全ての操作コマンド（例: U, D, L, R, 1）を記録するためのビルダ。
                             // 最後にまとめて標準出力に出力されます。
    Map<Integer, Box> boxes; // 全ての箱を、そのユニークなIDをキーとして管理するマップ。
                             // これにより、IDから素早く対応するBoxオブジェクトを取得できます。

    // グリッドの状態を保持する2次元配列。
    // 各マスに箱がある場合はその箱のIDを、箱がない場合は-1を格納します。
    // 社長が箱を拾うと、該当するマスの値は-1に更新されます。
    private int[][] grid; 

    /**
     * Solverクラスのコンストラクタ。
     * 問題の初期状態（グリッドサイズ、箱の重さ、耐久力）を設定し、
     * 全ての箱オブジェクトを生成して管理します。
     * @param n グリッドのサイズ
     * @param weights 各マスの箱の重さを表す2次元配列
     * @param durabilities 各マスの箱の耐久力を表す2次元配列
     */
    public Solver(int n, int[][] weights, int[][] durabilities) {
        this.N = n;
        this.px = 0; // 社長の初期位置は常に(0,0)です。
        this.py = 0;
        this.hand = new ArrayList<>(); // 手持ちの箱リストを初期化
        this.actions = new StringBuilder(); // アクション記録用のStringBuilderを初期化
        this.boxes = new HashMap<>(); // 箱管理用のマップを初期化
        this.grid = new int[N][N]; // グリッド状態管理用の配列を初期化

        int boxIdCounter = 0; // 箱にユニークなIDを割り当てるためのカウンター
        // 入力された重さ情報に基づいて、グリッド上の箱をBoxオブジェクトとして生成します。
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (weights[i][j] > 0) { // 重さが0より大きい場合、そのマスには箱が存在します。
                    // 新しいBoxオブジェクトを生成し、ID、位置、重さ、耐久力を設定します。
                    Box box = new Box(boxIdCounter, i, j, weights[i][j], durabilities[i][j]);
                    this.boxes.put(boxIdCounter, box); // 生成した箱をマップに追加
                    this.grid[i][j] = boxIdCounter; // グリッド上に箱のIDを記録
                    boxIdCounter++; // 次の箱のためにIDカウンターをインクリメント
                } else {
                    this.grid[i][j] = -1; // 箱がないマスは-1でマークします。
                }
            }
        }
    }

    /**
     * 問題解決の全プロセスを実行します。
     * 未輸送の箱がなくなるまで、一連の輸送サイクルを繰り返します。
     * 各サイクルでは、最適な箱の組み合わせを計画し、実行します。
     */
    public void solve() {
        // まだ輸送されていない箱のIDを管理するセット。
        // 初期状態では全ての箱のIDが含まれています。
        Set<Integer> remainingBoxIds = new HashSet<>(this.boxes.keySet());

        // 輸送すべき箱が残っている限り、このメインループを繰り返します。
        while (!remainingBoxIds.isEmpty()) {
            // 各輸送サイクルの開始時、社長は必ず(0,0)にいて、手は空である状態にします。
            this.moveTo(0, 0); // 現在位置から(0,0)へ移動し、その間のダメージを計算・適用
            this.hand.clear(); // 手持ちの箱を全て降ろします（スタックを空にする）

            // この現在の輸送サイクルで運ぶことを計画している箱のIDリスト。
            List<Integer> planIds = new ArrayList<>(); 

            // この内部ループでは、現在の輸送サイクルで一度に運べる最適な箱の組み合わせ（計画）を貪欲に構築します。
            // (0,0)からスタートし、複数の箱を連続して拾う経路を想定します。
            while (true) {
                int bestNextBoxId = -1; // 今回のイテレーションで計画に追加する最適な箱のIDを初期化
                // 評価値の最小値を保持。最初は可能な限り大きな値で初期化し、より良い（小さい）評価値が見つかれば更新します。
                double min_eval_value = Double.MAX_VALUE; 

                // 現在の計画において、最後に箱を拾った場所、または計画がまだ空の場合（最初の箱を探す場合）は社長の現在位置(0,0)
                int current_r = this.px;
                int current_c = this.py;
                if (!planIds.isEmpty()) {
                    // 計画に既に箱がある場合、最後に拾った箱の位置を現在の出発点とします。
                    Box lastBoxInPlan = this.boxes.get(planIds.get(planIds.size() - 1));
                    current_r = lastBoxInPlan.r;
                    current_c = lastBoxInPlan.c;
                }
                
                // 次に計画に追加する候補となる箱のIDリストを作成します。
                // まだ輸送されていない箱（remainingBoxIds）から、既に現在の計画に追加済みの箱（planIds）を除外します。
                Set<Integer> candidateIds = new HashSet<>(remainingBoxIds);
                candidateIds.removeAll(new HashSet<>(planIds));
                
                if (candidateIds.isEmpty()) {
                    // 候補となる箱がもうない場合、このサイクルの計画構築は終了です。
                    break; 
                }
                
                // 残っている全ての候補箱についてループし、現在の計画に追加可能かシミュレーションして評価します。
                for (int boxId : candidateIds) {
                    Box box = this.boxes.get(boxId); // 候補となる箱のオブジェクトを取得
                    // 社長の現在の出発点から候補の箱までのマンハッタン距離を計算します。
                    int dist = Math.abs(box.r - current_r) + Math.abs(box.c - current_c);
                    
                    // 候補の箱を現在の計画に追加した場合の「仮の新しい計画」を作成します。
                    List<Integer> nextPlan = new ArrayList<>(planIds);
                    nextPlan.add(boxId);

                    // この「仮の新しい計画」が、箱が潰れることなく最後まで実行可能かをisPlanFeasibleメソッドでシミュレーションチェックします。
                    if (isPlanFeasible(nextPlan)) {
                        // ★重要: 評価関数の計算★
                        // (移動距離) / (箱の重さ) を評価値とします。
                        // この値が小さいほど「効率が良い」と判断します。
                        // 例: 同じ移動距離なら、より重い箱を優先します（分母が大きいほど評価値が小さくなる）。
                        // 例: 同じ重さなら、より近い箱を優先します（分子が小さいほど評価値が小さくなる）。
                        // ※問題の制約上、箱の重さが0のケースは存在しないため、0除算の心配はありません。
                        double current_eval_value = (double)dist / box.w; 
                        
                        // 現在までに見つかった最も良い評価値 (min_eval_value) と比較し、
                        // より良い（小さい）評価値であれば、それを最良の候補として更新します。
                        if (current_eval_value < min_eval_value) {
                            min_eval_value = current_eval_value;
                            bestNextBoxId = boxId; // 最も評価値の良かった箱のIDを記録
                        }
                    }
                }

                if (bestNextBoxId != -1) {
                    // 最も評価値の良かった箱が見つかった場合、それをこのサイクルの確定計画に追加します。
                    planIds.add(bestNextBoxId);
                } else {
                    // 現在の状況で、安全に実行可能な追加の箱が見つからなかった場合、
                    // このサイクルの計画構築はこれ以上続けられないため、ループを終了します。
                    break; 
                }
            }
            
            // --- フォールバック戦略 ---
            // 上記の計画構築ループで、複数の箱を運ぶ計画が一つも立てられなかった場合（planIdsが空の場合）
            // かつ、まだ未輸送の箱が残っている場合、単独で運べる箱がないか再探索します。
            if (planIds.isEmpty() && !remainingBoxIds.isEmpty()) {
                double min_eval_value = Double.MAX_VALUE; // ここでも評価値で比較し、単独で運ぶ最も効率的な箱を探します。
                int singleTargetId = -1; // 単独で運ぶ最適な箱のIDを初期化

                // 未輸送の箱全てを対象に、単独で運ぶ計画が実行可能かチェックします。
                for (int boxId : remainingBoxIds) {
                    Box box = this.boxes.get(boxId);
                    // 社長の現在位置（0,0）からこの箱までの距離を計算
                    int dist = Math.abs(box.r - this.px) + Math.abs(box.c - this.py);
                    List<Integer> singleBoxPlan = new ArrayList<>();
                    singleBoxPlan.add(boxId); // その箱単独の計画を作成

                    // この単独の箱を運ぶ計画が、箱を潰さずに実行可能かisPlanFeasibleでチェックします。
                    if (isPlanFeasible(singleBoxPlan)) { 
                        double current_eval_value = (double)dist / box.w; // 単独輸送の場合も同様の評価関数を使用
                        if (current_eval_value < min_eval_value) {
                            min_eval_value = current_eval_value;
                            singleTargetId = boxId; // 最も評価値の良かった単独の箱を記録
                        }
                    }
                }
                if (singleTargetId != -1) {
                    // 単独で安全に運べる箱が見つかった場合、その箱を現在の計画に追加します。
                    planIds.add(singleTargetId); 
                } else {
                    // どの箱も単独ですら安全に運べない極端な状況の場合、
                    // 現在の戦略ではこれ以上箱を運べないため、無限ループを防ぐためにここでメインループを抜けます。
                    System.err.println("Warning: No box can be transported safely, even individually. Breaking loop.");
                    break; 
                }
            }

            // フォールバック戦略を試した後でも計画が空で、かつ未輸送の箱がまだ残っている場合、
            // これは現在の戦略では解決不可能な状態である可能性が高いです。
            // この場合も無限ループや不適切な動作を防ぐために、メインループを抜けます。
            if (planIds.isEmpty() && !remainingBoxIds.isEmpty()) {
                System.err.println("Error: Plan is empty but remaining boxes exist. Strategy might be stuck or unsuitable.");
                break;
            }

            // --- 確定した輸送計画の実行 ---
            // 策定された計画（planIdsに格納された箱の順序）に従って、実際に箱を運搬する操作を行います。
            for (int boxId : planIds) {
                Box box = this.boxes.get(boxId); // 運ぶ箱のオブジェクトを取得
                this.moveTo(box.r, box.c); // 社長を箱の場所へ移動させ、移動コマンドを記録し、手持ちの箱にダメージを適用します。
                this.pick(box);             // 箱を拾い上げ、手持ちに追加し、ピックアップコマンドを記録します。
                this.grid[box.r][box.c] = -1; // 拾った箱の場所は空になるため、グリッドの状態を更新します。
            }
            this.moveTo(0, 0); // 全ての計画された箱を拾い終えたら、最終的に(0,0)の原点へ帰還します。

            // このサイクルで輸送が完了した箱を、未輸送の箱リストから削除します。
            remainingBoxIds.removeAll(new HashSet<>(planIds));
        }

        // 全ての箱の輸送が完了した後、記録された全てのアクション（操作コマンド）を一度に標準出力に出力します。
        System.out.print(this.actions.toString());
    }

    /**
     * 与えられた輸送計画（箱のIDリスト）が、箱の耐久力を超えることなく実行可能かどうかをシミュレートします。
     * 社長は(0,0)から出発し、planIdsの箱を順に拾い、最終的に(0,0)へ帰還する経路を仮想的にたどります。
     * このメソッドは、実際の箱の状態を変更することなく、計画の実行可能性を評価します。
     * @param planIds このサイクルで運ぶ箱のIDリスト（順序が重要）
     * @return 計画が箱を潰さずに完了できる場合はtrue、途中で箱が破損する場合はfalse
     */
    private boolean isPlanFeasible(List<Integer> planIds) {
        if (planIds.isEmpty()) return true; // 計画が空であれば、何も移動しないので常に実行可能です。

        // シミュレーション用に、現在の全ての箱の状態（特に耐久力）をディープコピーして使用します。
        // これにより、isPlanFeasibleメソッドが実際の箱のデータに影響を与えることなく、独立してシミュレーションを行えます。
        Map<Integer, Box> simulationBoxes = new HashMap<>();
        for (Map.Entry<Integer, Box> entry : this.boxes.entrySet()) {
            simulationBoxes.put(entry.getKey(), new Box(entry.getValue())); // Boxのコピーコンストラクタを使用して、各Boxオブジェクトを個別にコピーします。
        }

        int current_r_sim = 0; // シミュレーション中の社長の現在位置の行座標。初期値は(0,0)です。
        int current_c_sim = 0; // シミュレーション中の社長の現在位置の列座標。
        List<Box> tempHand_sim = new ArrayList<>(); // シミュレーション中の手持ちの箱スタック。

        // 計画内の各箱について、その箱を拾いに行くまでの経路と、拾った後の状態をシミュレートします。
        for (int pid : planIds) {
            Box nextBox = simulationBoxes.get(pid); // 次に拾う予定の箱のオブジェクト（シミュレーション用コピー）を取得
            // 現在のシミュレーション上の社長の位置から、次に拾う箱の場所までのマンハッタン距離を計算します。
            int dist = Math.abs(nextBox.r - current_r_sim) + Math.abs(nextBox.c - current_c_sim);

            // この移動による手持ちの箱へのダメージをシミュレーションします。
            long weightOnTop_sim = 0; // 手持ちスタックの一番上にある箱の重さ（その下の箱に加わる荷重）を計算するための変数
            // 手持ちスタックの底から順に（つまり、先に拾った箱から順に、上に積まれた箱の重さを考慮しながら）ダメージを計算します。
            // スタックの一番上にある箱にはその上に積まれた箱がないため、重さがかかりません。
            for (int i = tempHand_sim.size() - 1; i >= 0; i--) {
                Box heldBox_sim = tempHand_sim.get(i); // 手に持っている箱（シミュレーション用コピー）
                // この箱にかかるダメージは、「この箱より上にある箱の総重量」と「今回の移動距離」の積で計算されます。
                heldBox_sim.d_current -= weightOnTop_sim * dist;
                
                // ★重要な破損チェック★
                // シミュレーション中に箱の耐久力が0以下になった場合、その箱は潰れてしまいます。
                // この計画は実行不可能と判断し、直ちにfalseを返してシミュレーションを中断します。
                if (heldBox_sim.d_current <= 0) {
                    // デバッグ用の出力例（本番環境では通常コメントアウト）
                    // System.err.println("DEBUG: Simulating box " + heldBox_sim.id + " crushed during move to " + nextBox.r + "," + nextBox.c);
                    return false; 
                }
                // 次の箱のために、現在の箱の重さを「その上にある箱の総重量」に加算します。
                weightOnTop_sim += heldBox_sim.w;
            }

            tempHand_sim.add(nextBox); // 次の箱を拾って、シミュレーション上の手持ちスタックに追加します。
            current_r_sim = nextBox.r; // シミュレーション上の社長の現在位置を、拾った箱の場所へ更新します。
            current_c_sim = nextBox.c;
        }

        // 計画内の全ての箱を拾い終えた後、最後に(0,0)の原点へ帰還する経路のシミュレーションを行います。
        // この帰還経路でも、手持ちの箱にダメージがかかる可能性があるため、チェックが必要です。
        int distToOrigin = Math.abs(0 - current_r_sim) + Math.abs(0 - current_c_sim);
        weightOnTop_sim = 0; // 帰還経路のための重さ計算をリセット
        // 同様に、手持ちスタックの底から順にダメージを適用します。
        for (int i = tempHand_sim.size() - 1; i >= 0; i--) {
            Box heldBox_sim = tempHand_sim.get(i);
            heldBox_sim.d_current -= weightOnTop_sim * distToOrigin; // ダメージ計算
            
            // ★重要な破損チェック★
            // 帰還中に箱の耐久力が0以下になった場合も、計画は実行不可能と判断しfalseを返します。
            if (heldBox_sim.d_current <= 0) {
                // デバッグ用の出力例
                // System.err.println("DEBUG: Simulating box " + heldBox_sim.id + " crushed during move to origin (0,0)");
                return false; 
            }
            weightOnTop_sim += heldBox_sim.w; // 次の箱のために重さを加算
        }

        // シミュレーションの全行程（箱を拾いに行く移動と、原点への帰還移動）で、どの箱も潰れなかった場合、
        // この計画は安全に実行可能であると判断し、trueを返します。
        return true; 
    }

    /**
     * 指定された座標へ高橋社長を移動させ、その間の操作コマンドを記録し、
     * 手持ちの箱に移動によるダメージを適用します。
     * このメソッドは実際の移動とダメージ計算を行います。
     * @param tr 移動先の行座標
     * @param tc 移動先の列座標
     */
    private void moveTo(int tr, int tc) {
        int dr = tr - this.px; // 行方向の移動量 (正なら下、負なら上)
        int dc = tc - this.py; // 列方向の移動量 (正なら右、負なら左)

        int moveDist = Math.abs(dr) + Math.abs(dc); // マンハッタン距離による移動総距離
        if (moveDist == 0) return; // 移動距離が0の場合、何もする必要がないので処理を終了します。

        // 移動コマンドを actions StringBuilder に追加します。
        // U, D, L, R の順序は任意ですが、ここでは縦移動を先に、次に横移動を行います。
        if (dr > 0) { // 下方向へ移動する場合
            for (int i = 0; i < dr; i++) actions.append("D\n"); // Dコマンドをdr回追加
        } else { // 上方向へ移動する場合 (drが負の値なので-drで回数を計算)
            for (int i = 0; i < -dr; i++) actions.append("U\n"); // Uコマンドを-dr回追加
        }

        if (dc > 0) { // 右方向へ移動する場合
            for (int i = 0; i < dc; i++) actions.append("R\n"); // Rコマンドをdc回追加
        } else { // 左方向へ移動する場合 (dcが負の値なので-dcで回数を計算)
            for (int i = 0; i < -dc; i++) actions.append("L\n"); // Lコマンドを-dc回追加
        }

        // 実際の移動による手持ちの箱へのダメージ計算と適用を行います。
        long weightOnTop = 0; // 手持ちスタックの一番上にある箱の重さ（その下の箱に加わる荷重）
        // シミュレーション時と同様に、手持ちスタックの底から順に実際のダメージを適用します。
        for (int i = this.hand.size() - 1; i >= 0; i--) {
            Box box = this.hand.get(i); // 手に持っている箱（実際のオブジェクト）
            box.d_current -= weightOnTop * moveDist; // ダメージ計算し、現在の耐久力を更新します。
            
            // isPlanFeasibleメソッドが正しく機能していれば、ここで箱が潰れることは原理的にないはずです。
            // もしここに入った場合、何らかのロジックエラーや予期せぬ状態が発生している可能性が高いため、エラー警告を出力します。
            if (box.d_current <= 0) {
                System.err.println("Error: Box " + box.id + " crushed during actual movement!");
                // 実際の問題ではここでプログラムを終了するか、追加のハンドリングが必要になる場合があります。
            }
            weightOnTop += box.w; // 次の箱のダメージ計算のために、現在の箱の重さを加算します。
        }
        // 社長の現在位置を、移動先の座標に更新します。
        this.px = tr;
        this.py = tc;
    }

    /**
     * 社長が現在いるマスにある箱を拾い上げる操作を行い、
     * その箱を手持ちのスタックに追加し、対応するコマンドを記録します。
     * @param box 拾う対象のBoxオブジェクト
     */
    private void pick(Box box) {
        this.hand.add(box);         // 拾った箱を手持ちスタックの最上部に追加します。
        this.actions.append("1\n"); // 操作1（箱を拾う）のコマンドをactionsに記録します。
    }
}