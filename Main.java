import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * メインクラス。入力の読み込みとSolverの実行を行います。
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // 高速な入力処理のためのBufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        int N = 20; // 問題の制約でNは常に20
        br.readLine(); // Nの行は読み飛ばし（入力フォーマットによる）

        int[][] weights = new int[N][N];
        int[][] durabilities = new int[N][N];

        // 箱の重さを読み込む
        for (int i = 0; i < N; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            for (int j = 0; j < N; j++) {
                weights[i][j] = Integer.parseInt(st.nextToken());
            }
        }
        // 箱の耐久力を読み込む
        for (int i = 0; i < N; i++) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            for (int j = 0; j < N; j++) {
                durabilities[i][j] = Integer.parseInt(st.nextToken());
            }
        }

        // Solverインスタンスを生成し、問題を解く
        Solver solver = new Solver(N, weights, durabilities);
        solver.solve();
    }
}

/**
 * ダンボール箱の情報を保持するクラス。
 * ID、現在位置、重さ、初期耐久力、現在の耐久力を持つ。
 */
class Box {
    int id;          // 箱の識別ID
    int r, c;        // 箱の現在位置（行、列）
    int w;           // 箱の重さ
    long d_initial;  // 箱の初期耐久力
    long d_current;  // 箱の現在の耐久力（移動で減少する）

    public Box(int id, int r, int c, int w, int d) {
        this.id = id;
        this.r = r;
        this.c = c;
        this.w = w;
        this.d_initial = d;
        this.d_current = d; // 最初は初期耐久力が現在の耐久力
    }

    // コピーコンストラクタ
    // シミュレーションなどで箱の状態を一時的に変更したい場合に、元の箱に影響を与えないようディープコピーを作成する
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
 * 問題解決のメインロジックを担うクラス。
 * 高橋社長の移動、箱のピックアップ、計画のシミュレーションなどを行う。
 */
class Solver {
    final int N;             // グリッドのサイズ
    int px, py;              // 高橋社長の現在位置（行、列）
    List<Box> hand;          // 手に持っている箱のスタック（一番上がhand.get(hand.size()-1)）
    StringBuilder actions;   // 出力する操作コマンドを記録するビルダ
    Map<Integer, Box> boxes; // 全ての箱をIDをキーとして管理するマップ

    // グリッドの状態を保持（箱があるマスは箱のID、ないマスは-1）
    // 社長が箱を拾うとこのグリッドも更新される
    private int[][] grid; 

    public Solver(int n, int[][] weights, int[][] durabilities) {
        this.N = n;
        this.px = 0; // 社長の初期位置は(0,0)
        this.py = 0;
        this.hand = new ArrayList<>();
        this.actions = new StringBuilder();
        this.boxes = new HashMap<>();
        this.grid = new int[N][N]; // グリッドを初期化

        int boxIdCounter = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (weights[i][j] > 0) { // 重さが0より大きい場合、そこに箱がある
                    Box box = new Box(boxIdCounter, i, j, weights[i][j], durabilities[i][j]);
                    this.boxes.put(boxIdCounter, box);
                    this.grid[i][j] = boxIdCounter; // 箱のIDをグリッドに設定
                    boxIdCounter++;
                } else {
                    this.grid[i][j] = -1; // 箱がないマスは-1で表す
                }
            }
        }
    }

    /**
     * 問題解決の全プロセスを実行する。
     * 未輸送の箱がなくなるまで、輸送サイクルを繰り返す。
     */
    public void solve() {
        // まだ輸送されていない箱のIDを管理するセット
        Set<Integer> remainingBoxIds = new HashSet<>(this.boxes.keySet());

        // 輸送すべき箱が残っている限りループ
        while (!remainingBoxIds.isEmpty()) {
            // 各輸送サイクルの開始時、社長は(0,0)にいて、手は空である状態にする
            this.moveTo(0, 0); // (0,0)へ移動
            this.hand.clear(); // 手持ちの箱を空にする

            List<Integer> planIds = new ArrayList<>(); // このサイクルで運ぶ箱のIDリスト

            // このサイクルで運ぶ箱を貪欲に選択していくループ
            // (0,0)からスタートして、複数の箱を連続して拾う計画を立てる
            while (true) {
                int bestNextBoxId = -1;
                // ★変更点★ 評価値の最小値を保持。初期値は最大値にしておく。
                // 距離だけでなく、箱の重さも考慮した評価値を使う。
                double min_eval_value = Double.MAX_VALUE; 

                // 現在の計画での、最後に箱を拾った場所（または、計画が空なら社長の現在位置(0,0)）
                int current_r = this.px;
                int current_c = this.py;
                if (!planIds.isEmpty()) {
                    Box lastBoxInPlan = this.boxes.get(planIds.get(planIds.size() - 1));
                    current_r = lastBoxInPlan.r;
                    current_c = lastBoxInPlan.c;
                }
                
                // 次に候補となる箱のIDリスト（まだ運んでおらず、かつこのサイクルの計画に入っていない箱）
                Set<Integer> candidateIds = new HashSet<>(remainingBoxIds);
                candidateIds.removeAll(new HashSet<>(planIds));
                
                if (candidateIds.isEmpty()) {
                    // 候補となる箱がもうない場合、このサイクルの計画は終了
                    break; 
                }
                
                // 全ての候補箱について、このサイクルの計画に追加可能かシミュレーションして評価
                for (int boxId : candidateIds) {
                    Box box = this.boxes.get(boxId);
                    // 社長の現在位置から候補の箱までのマンハッタン距離
                    int dist = Math.abs(box.r - current_r) + Math.abs(box.c - current_c);
                    
                    // 候補の箱を現在の計画に追加した場合の新しい計画
                    List<Integer> nextPlan = new ArrayList<>(planIds);
                    nextPlan.add(boxId);

                    // この新しい計画が、箱が潰れることなく実行可能かシミュレーションチェック
                    if (isPlanFeasible(nextPlan)) {
                        // ★変更点★ 評価関数の計算: (移動距離) / (箱の重さ)
                        // この値が小さいほど「効率が良い」と判断する
                        // （例: 同じ距離なら重い箱を優先、同じ重さなら近い箱を優先）
                        // ※箱の重さが0のケースはここでは考慮しない（問題の制約上、重さ>0）
                        double current_eval_value = (double)dist / box.w; 
                        
                        // 現在見つかっている最も良い評価値と比べて、より良い（小さい）評価値であれば更新
                        if (current_eval_value < min_eval_value) {
                            min_eval_value = current_eval_value;
                            bestNextBoxId = boxId; // 最も評価値の良かった箱を記録
                        }
                    }
                }

                if (bestNextBoxId != -1) {
                    // 最も評価値の良かった箱をこのサイクルの計画に追加
                    planIds.add(bestNextBoxId);
                } else {
                    // 実行可能な追加の箱が見つからなかった場合、このサイクルの計画は終了
                    break; 
                }
            }
            
            // フォールバック処理: 計画が空の場合（どの箱も複数個運べないと判断された場合）
            // まだ未輸送の箱が残っていれば、その中で最も評価値の良い箱を1つだけ運ぶ
            if (planIds.isEmpty() && !remainingBoxIds.isEmpty()) {
                double min_eval_value = Double.MAX_VALUE; // ここでも評価値で比較
                int singleTargetId = -1;
                for (int boxId : remainingBoxIds) {
                    Box box = this.boxes.get(boxId);
                    int dist = Math.abs(box.r - this.px) + Math.abs(box.c - this.py);
                    List<Integer> singleBoxPlan = new ArrayList<>();
                    singleBoxPlan.add(boxId); // その箱単独の計画を作成
                    if (isPlanFeasible(singleBoxPlan)) { // 単独で運ぶ計画が実行可能かチェック
                         double current_eval_value = (double)dist / box.w;
                         if (current_eval_value < min_eval_value) {
                            min_eval_value = current_eval_value;
                            singleTargetId = boxId;
                        }
                    }
                }
                if (singleTargetId != -1) {
                    planIds.add(singleTargetId); // 単独で運べる箱があれば計画に追加
                } else {
                    // どの箱も単独ですら安全に運べない場合、この戦略ではこれ以上箱を運べない
                    // 無限ループを防ぐためにも、ここでループを抜ける
                    System.err.println("Warning: No box can be transported safely, even individually. Breaking loop.");
                    break; 
                }
            }

            // フォールバック後も計画が空で、かつ未輸送の箱が残っている場合
            // これは、現在の戦略では解決不能な状態を示唆しているため、ループを抜ける
            if (planIds.isEmpty() && !remainingBoxIds.isEmpty()) {
                System.err.println("Error: Plan is empty but remaining boxes exist. Strategy might be stuck or unsuitable.");
                break;
            }

            // 確定した輸送計画を実行
            for (int boxId : planIds) {
                Box box = this.boxes.get(boxId);
                this.moveTo(box.r, box.c); // 箱の場所へ移動
                this.pick(box);             // 箱を拾う
                this.grid[box.r][box.c] = -1; // 拾った箱の場所は空になるのでグリッドを更新
            }
            this.moveTo(0, 0); // 全ての箱を拾い終えたら、(0,0)へ帰還
            
            // 輸送した箱を未輸送の箱リストから削除
            remainingBoxIds.removeAll(new HashSet<>(planIds));
        }

        // 全てのアクション（操作コマンド）を一度に出力
        System.out.print(this.actions.toString());
    }

    /**
     * 与えられた輸送計画（箱IDリスト）が耐久力的に可能かシミュレートする。
     * 社長は(0,0)から出発し、planIdsの箱を順に拾い、最終的に(0,0)へ帰還する経路をシミュレート。
     * @param planIds このサイクルで運ぶ箱のIDリスト
     * @return 計画が箱を潰さずに実行可能であればtrue、そうでなければfalse
     */
    private boolean isPlanFeasible(List<Integer> planIds) {
        if (planIds.isEmpty()) return true; // 計画が空なら常に実行可能

        // シミュレーション用に、実際の箱の状態（耐久力など）をコピーして使用する
        // これにより、元の箱の状態がシミュレーション中に変更されるのを防ぐ
        Map<Integer, Box> simulationBoxes = new HashMap<>();
        for (Map.Entry<Integer, Box> entry : this.boxes.entrySet()) {
            simulationBoxes.put(entry.getKey(), new Box(entry.getValue())); // Boxのコピーコンストラクタを使用
        }

        int current_r_sim = 0; // シミュレーション中の社長の現在位置
        int current_c_sim = 0;
        List<Box> tempHand_sim = new ArrayList<>(); // シミュレーション中の手持ちの箱スタック

        // 計画内の各箱についてシミュレーション
        for (int pid : planIds) {
            Box nextBox = simulationBoxes.get(pid); // 次に拾う箱
            // 現在地から次の箱の場所までの移動距離
            int dist = Math.abs(nextBox.r - current_r_sim) + Math.abs(nextBox.c - current_c_sim);

            // 移動による手持ちの箱へのダメージをシミュレーション
            long weightOnTop_sim = 0; // 手持ちスタックの一番上にある箱の重さ
            // 手持ちスタックの底から順に（手前にある箱から順に）ダメージを計算
            // スタックの一番上にある箱には重さがかからないため、その下の箱から計算
            for (int i = tempHand_sim.size() - 1; i >= 0; i--) {
                Box heldBox_sim = tempHand_sim.get(i); // 手に持っている箱
                // この箱にかかるダメージ = この箱より上にある箱の総重量 * 移動距離
                heldBox_sim.d_current -= weightOnTop_sim * dist;
                
                // ★修正点★ 耐久力が0以下になったら、この計画は不可能と判断しfalseを返す
                if (heldBox_sim.d_current <= 0) {
                    // System.err.println("DEBUG: Simulating box " + heldBox_sim.id + " crushed during move to " + nextBox.r + "," + nextBox.c);
                    return false; 
                }
                // 現在の箱より上にある箱の総重量に、現在の箱の重さを加える
                weightOnTop_sim += heldBox_sim.w;
            }

            tempHand_sim.add(nextBox); // 次の箱を拾って手持ちに追加
            current_r_sim = nextBox.r; // 社長の現在位置を更新
            current_c_sim = nextBox.c;
        }

        // 最後の区間：全ての箱を拾い終えた後、(0,0)へ帰還する経路のシミュレーション
        int distToOrigin = Math.abs(0 - current_r_sim) + Math.abs(0 - current_c_sim);
        long weightOnTop_sim = 0;
        for (int i = tempHand_sim.size() - 1; i >= 0; i--) {
            Box heldBox_sim = tempHand_sim.get(i);
            heldBox_sim.d_current -= weightOnTop_sim * distToOrigin;
            
            // ★修正点★ 耐久力が0以下になったら、この計画は不可能と判断しfalseを返す
            if (heldBox_sim.d_current <= 0) {
                // System.err.println("DEBUG: Simulating box " + heldBox_sim.id + " crushed during move to origin (0,0)");
                return false; 
            }
            weightOnTop_sim += heldBox_sim.w;
        }

        return true; // 全ての箱がシミュレーション中に潰れなかったため、計画は実行可能
    }

    /**
     * 指定座標へ移動し、コマンドを記録し、手持ちの箱にダメージを与える。
     * @param tr 移動先の行座標
     * @param tc 移動先の列座標
     */
    private void moveTo(int tr, int tc) {
        int dr = tr - this.px; // 行方向の移動量
        int dc = tc - this.py; // 列方向の移動量

        int moveDist = Math.abs(dr) + Math.abs(dc); // 移動総距離（マンハッタン距離）
        if (moveDist == 0) return; // 移動距離が0なら何もしない

        // 移動コマンドを actions に追加
        // U, D, L, R の順序は任意だが、ここでは縦移動、横移動の順
        if (dr > 0) for (int i = 0; i < dr; i++) actions.append("D\n"); // 下へ移動
        else for (int i = 0; i < -dr; i++) actions.append("U\n"); // 上へ移動

        if (dc > 0) for (int i = 0; i < dc; i++) actions.append("R\n"); // 右へ移動
        else for (int i = 0; i < -dc; i++) actions.append("L\n"); // 左へ移動

        // 実際の移動による手持ちの箱へのダメージ計算と適用
        long weightOnTop = 0; // 手持ちスタックの一番上にある箱の重さ
        // シミュレーション時と同様に、手持ちスタックの底から順にダメージを適用
        for (int i = this.hand.size() - 1; i >= 0; i--) {
            Box box = this.hand.get(i);
            box.d_current -= weightOnTop * moveDist; // ダメージ計算
            
            // isPlanFeasibleが正しく機能していれば、ここで箱が潰れることはないはずだが、念のため警告を出力
            if (box.d_current <= 0) {
                System.err.println("Error: Box " + box.id + " crushed during actual movement!");
            }
            weightOnTop += box.w; // 次の箱のために重さを加算
        }
        // 社長の現在位置を更新
        this.px = tr;
        this.py = tc;
    }

    /**
     * 社長がいるマスにある箱を拾う操作を行い、手持ちに追加し、コマンドを記録する。
     * @param box 拾う対象の箱オブジェクト
     */
    private void pick(Box box) {
        this.hand.add(box);       // 箱を手持ちスタックに追加
        this.actions.append("1\n"); // 操作1（拾う）のコマンドを記録
    }
}