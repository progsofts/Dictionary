package com.progsoft.dictionary.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import com.progsoft.dictionary.R;
import com.progsoft.dictionary.util.JsonUtil;
import com.progsoft.dictionary.util.PathParser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class ChineseCharacterView extends View {
    public static final int MSG = 1;

    private final String TAG = "progsofts" + ChineseCharacterView.class.getSimpleName();

    private TimerHandler animateHandler = new TimerHandler(this);

    private float animateValue;

    private int arrowSize = dp2px(5.0F);

    private Region curClipRegion;

    private Path curDrawPath;

    private PathMeasure curDrawPathMeasure;

    private int curDrawingIndex;

    private boolean curDrawingPathValid;

    private Path curMedianPath;

    private RectF curPathRectF;

    /**
     * 设置当前笔画的区域，用于判断是否按照笔画去写字
     */

    private Region curPathRegion;

    private Path curStrokePath;

    private int curWrongTouchCount;

    private Paint dashPaint = new Paint(1);

    private float downX;

    private float downY;

    private boolean drawGrid = true;

    private int fixedSize = 1050;

    private String pinyin = "";

    /**
     * 初始田字格的笔画属性
     */
    private Paint gridPaint = new Paint(1);

    /**
     * 初始田字格的框框边界
     */
    private RectF gridRect = new RectF();

    private int gridWidth = dp2px(1.0F);

    private boolean isPause;

    private boolean isStop;

    private float lastX;

    private float lastY;

    private Paint maskPaint = new Paint(1);

    private final float[] maskPos;

    private float maskRadius;

    private final float[] maskTan;

    private final int maxWrongTouch = 60;

    private List<String> medianOriPaths;

    private Paint medianPaint = new Paint(1);

    /**
     * 笔画图，已经转换完成的  <br/>
     * redraw选择清零 True
     */
    private final ArrayList<Path> medianPaths = new ArrayList();

    private final long messageTimeGap = 60L;

    private final long messageTimeNextStrokeGap = 60L;

    private boolean midDrawingValid;

    /**
     * 字体倒置设置开关
     */
    private boolean needShift = true;
    /**
     * 原始图片的镜像，缩放模板
     */
    private Matrix pathMatrix = new Matrix();
    /**
     * 整体笔画展示信息，List-List-PointF<br/>
     * 通过读取文件设置进入
     */
    private List<? extends List<? extends PointF>> pathPoints;
    /**
     * ReDraw 清零
     */
    private List<Path> preDrawPaths = (List) new ArrayList();

    private int rectSize;

    private float shiftFactor = 0.76F;

    private int strokeColor = Color.parseColor("#bcbcbc"); //灰色
    /**
     * 通知画几笔，和是否结束， 没有注册
     */
    private StrokeDrawListener strokeDrawCompleteListener;
    /**
     * 整体笔画图信息， List<String>， 通过读取文件设置进入
     */
    private List<String> strokeInfo;

    private Paint strokePaint = new Paint(1);
    /**
     * 字体底图，已经转换完成的<br/>
     * redraw选择清零 True
     */
    private final ArrayList<Path> strokePaths = new ArrayList();

    private int touchColor = Color.parseColor("#ffba00"); //黄色

    private Paint touchPaint = new Paint(1);

    private float touchWidth;

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public ChineseCharacterView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        TypedArray typedArray = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.ChineseCharacterView);
        this.drawGrid = typedArray.getBoolean(R.styleable.ChineseCharacterView_drawgrid, true);
        this.autoDraw = typedArray.getBoolean(R.styleable.ChineseCharacterView_autodraw, false);
        typedArray.recycle();
        this.gridPaint.setColor(-7829368);
        this.gridPaint.setStyle(Paint.Style.STROKE);
        this.gridPaint.setStrokeWidth(this.gridWidth);
        this.dashPaint.setColor(-7829368);
        this.dashPaint.setStyle(Paint.Style.STROKE);
        this.dashPaint.setStrokeWidth(dp2px(1.0F));
        this.dashPaint.setPathEffect((PathEffect) new DashPathEffect(new float[]{5.0F, 5.0F}, 0.0F));
        this.medianPaint.setColor(this.touchColor);
        this.medianPaint.setStyle(Paint.Style.STROKE);
        this.medianPaint.setStrokeJoin(Paint.Join.ROUND);
        this.medianPaint.setStrokeCap(Paint.Cap.ROUND);
        this.medianPaint.setStrokeWidth(dp2px(2.0F));
        this.strokePaint.setColor(this.strokeColor);
        this.strokePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.strokePaint.setStrokeJoin(Paint.Join.ROUND);
        this.strokePaint.setStrokeCap(Paint.Cap.ROUND);
        this.touchPaint.setColor(this.touchColor);
        this.touchPaint.setStyle(Paint.Style.STROKE);
        this.touchPaint.setXfermode((Xfermode) new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        this.maskPaint.setColor(this.touchColor);
        this.maskPaint.setStyle(Paint.Style.FILL);
        this.maskPaint.setXfermode((Xfermode) new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        setLayerType(1, null);
        this.curPathRectF = new RectF();
        this.curPathRegion = new Region();
        this.curClipRegion = new Region();
        this.midDrawingValid = true;
        this.maskPos = new float[2];
        this.maskTan = new float[2];

        init(); // view出现初始化  字体底图
    }

    private void init() {
        initStrokePaths();

    }

    private void drawArrow(Canvas paramCanvas, Path paramPath) {
        float[] arrayOfFloat1 = new float[2];
        float[] arrayOfFloat2 = new float[2];
        PathMeasure pathMeasure = new PathMeasure(paramPath, false);
        pathMeasure.getPosTan(pathMeasure.getLength(), arrayOfFloat1, arrayOfFloat2);
        double d1 = Math.atan2(arrayOfFloat2[1], arrayOfFloat2[0]) * 180.0F / Math.PI;
        double d2 = 15;
        double d3 = '`';
        double d7 = (d1 + d2) * Math.PI / d3;
        double d6 = (d1 - d2) * Math.PI / d3;
        d1 = arrayOfFloat1[0];
        d2 = this.arrowSize;
        d3 = Math.cos(d7);
        double d4 = arrayOfFloat1[1];
        double d5 = this.arrowSize;
        d7 = Math.sin(d7);
        double d8 = arrayOfFloat1[0];
        double d9 = this.arrowSize;
        double d10 = Math.cos(d6);
        double d11 = arrayOfFloat1[1];
        double d12 = this.arrowSize;
        d6 = Math.sin(d6);
        Path path = new Path();
        path.moveTo((float) (d1 - d2 * d3), (float) (d4 - d5 * d7));
        path.lineTo(arrayOfFloat1[0], arrayOfFloat1[1]);
        path.lineTo((float) (d8 - d9 * d10), (float) (d11 - d12 * d6));
        if (paramCanvas != null) {
            paramCanvas.drawPath(path, this.medianPaint);
        }
    }

    /**
     *
     * @param paramFloat1 x
     * @param paramFloat2 y
     * @return 判断终止笔画在一个范围内
     */
    private boolean inEndPointRange(float paramFloat1, float paramFloat2) {
        if (this.curDrawPathMeasure == null) {
            return false;
        }
        float[] arrayOfFloat1 = new float[2];
        float[] arrayOfFloat2 = new float[2];
        PathMeasure pathMeasure1 = this.curDrawPathMeasure; //用于判断终止笔画在范围内
        PathMeasure pathMeasure2 = this.curDrawPathMeasure; //用于判断终止笔画在范围内
        pathMeasure1.getPosTan(pathMeasure2.getLength(), arrayOfFloat1, arrayOfFloat2); // 长度 位置， 位置 坐标， 位置处切线
        paramFloat1 -= arrayOfFloat1[0];
        paramFloat2 -= arrayOfFloat1[1];
        return (Math.sqrt((paramFloat1 * paramFloat1 + paramFloat2 * paramFloat2)) <= this.touchWidth);
    }

    /**
     *
     * @param paramFloat1 x
     * @param paramFloat2 y
     * @return 判断中间笔画在笔画范围内
     */
    private boolean inMidPointRange(float paramFloat1, float paramFloat2) {
        return this.curPathRegion.contains((int) paramFloat1, (int) paramFloat2);
    }

    /**
     *
     * @param paramFloat1 x
     * @param paramFloat2 y
     * @return 判断起始笔画在一个范围内
     */
    private boolean inStartPointRange(float paramFloat1, float paramFloat2) {
        if (this.curDrawPathMeasure == null) {
            return false;
        }
        float[] arrayOfFloat1 = new float[2];
        float[] arrayOfFloat2 = new float[2];
        PathMeasure pathMeasure = this.curDrawPathMeasure;  //用于判断终止笔画在范围内
        pathMeasure.getPosTan(0.0F, arrayOfFloat1, arrayOfFloat2);
        paramFloat1 -= arrayOfFloat1[0];
        paramFloat2 -= arrayOfFloat1[1];
        return (Math.sqrt((paramFloat1 * paramFloat1 + paramFloat2 * paramFloat2)) <= this.touchWidth);
    }

    private static Path parser(String paramString) {
        Path path = PathParser.createPathFromPathData(paramString);
        Log.e("progs-path", "<path d=\"" + paramString + "\"/>");
        path.setFillType(Path.FillType.WINDING);
        return path;
    }

    private void initStrokePaths() {
        try {
            List<String> list = this.strokeInfo;
            if (list != null) {
                this.strokePaths.clear();
                for (String str : list) {
                    Path path = parser(str);
                    path.transform(this.pathMatrix); //字体底图 变化大小和镜像， 字体获取的是上下镜像图
                    this.strokePaths.add(path);
                }
            }
            return;
        } catch (Exception exception) {
            return;
        }
    }

    /**
     * 启动动画画图功能
     * @param paramFloat
     */
    private void setAnimateValue(float paramFloat) {
        float f;
        PathMeasure pathMeasure = this.curDrawPathMeasure; //*****每一笔的长度， 输入参数是当前处理到这一笔的哪里了
        if (pathMeasure != null) {
            f = pathMeasure.getLength();  // 获取笔画长度， 用于动态显示， 按照步长来逐步画完
        } else {
            f = 0.0F;
        }
        if (paramFloat >= f) { //这一笔写完了
            Log.e("progs", "done");
            this.animateValue = 0.0F;
            this.curDrawingIndex++;
            if (this.curDrawingIndex >= this.medianPaths.size()) {
                if (this.curDrawingIndex == this.medianPaths.size()) {
                    StrokeDrawListener strokeDrawListener = this.strokeDrawCompleteListener;
                    if (strokeDrawListener != null) {
                        strokeDrawListener.onStrokeDrawComplete();
                    }
                } else {
                    this.curDrawingIndex = this.medianPaths.size();
                }
            } else {
                Path path = this.curDrawPath;
                if (path != null) {
                    this.preDrawPaths.add(path);
                }
                this.curDrawPath = new Path();
                this.curMedianPath = (Path) this.medianPaths.get(this.curDrawingIndex);
                PathMeasure pathMeasure1 = this.curDrawPathMeasure;  // 自动画模式， 用于重写设置下一笔 中线的测量
                if (pathMeasure1 != null) {
                    pathMeasure1.setPath(this.curMedianPath, false);
                }
                this.animateHandler.sendMessageDelayed(Message.obtain((Handler) this.animateHandler, 1), this.messageTimeNextStrokeGap);
            }
        } else { //这一笔还没写完
            long l;
            this.animateValue = paramFloat;
            pathMeasure = this.curDrawPathMeasure; // 用于设置该一笔动态画的当前位置
            if (pathMeasure != null) {  //***************重要 这里取笔画的轨迹 0~ 中间某个阶段的曲线， 作为划线动画演示， 第三个参数为输出参数
                pathMeasure.getSegment(0.0F, paramFloat, this.curDrawPath, true);
            }
            if (this.curDrawingIndex == 0 && paramFloat == 0.0F) {
                l = this.messageTimeNextStrokeGap;
            } else {
                l = this.messageTimeGap;
            }
            this.animateHandler.sendMessageDelayed(Message.obtain((Handler) this.animateHandler, 1), l);
        }
        invalidate();
    }

    public final void clear() {
        this.strokePaths.clear();
        this.medianPaths.clear();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.animateHandler.removeMessages(1);
    }


    @Override
    protected void onDraw(Canvas paramCanvas) {
        try {
            super.onDraw(paramCanvas);
            if (this.drawGrid) { // 画田字格， 必画
                if (paramCanvas != null) {
                    paramCanvas.drawRect(this.gridRect, this.gridPaint);
                }
                if (paramCanvas != null) {
                    paramCanvas.drawLine(0.0F, (this.rectSize / 2), this.rectSize, (this.rectSize / 2), this.dashPaint);
                }
                if (paramCanvas != null) {
                    paramCanvas.drawLine((this.rectSize / 2), 0.0F, (this.rectSize / 2), this.rectSize, this.dashPaint);
                }
                if (paramCanvas != null) {
                    paramCanvas.drawLine(0.0F, 0.0F, this.rectSize, this.rectSize, this.dashPaint);
                }
                if (paramCanvas != null) {
                    paramCanvas.drawLine(0.0F, this.rectSize, this.rectSize, 0.0F, this.dashPaint);
                }
            }
            boolean bool = this.autoDraw;
            Log.e("progs", "autoDraw:" + autoDraw + " showMedian:" + showMedian);
            int j = 0;
            int i = 0;
            if (bool) {
                if (this.strokePaths.size() == 0) { // 没有底图什么都别干
                    return;
                }
                if (this.curDrawingIndex < this.strokePaths.size()) { // 中间图画笔画，从没画完的地方开始， 画灰色
                    int k = this.curDrawingIndex + 1;
                    j = this.strokePaths.size();
                    while (k < j) {
                        this.curStrokePath = (Path) this.strokePaths.get(k);
                        this.strokePaint.setColor(this.strokeColor);
                        Path path = this.curStrokePath;
                        if (path != null && paramCanvas != null) {
                            paramCanvas.drawPath(path, this.strokePaint);
                        }
                        k++;
                    }
                    if (paramCanvas != null) { // 保存一个图层，这样下面srcin的处理， 就只有下面这一笔的处理了，这个很关键，所以不能和上面合并划线
                        k = paramCanvas.saveLayer(0.0F, 0.0F, this.rectSize, this.rectSize, null, Canvas.ALL_SAVE_FLAG);
                    } else {
                        k = 0;
                    }
                    this.strokePaint.setColor(this.strokeColor);
                    this.curStrokePath = (Path) this.strokePaths.get(this.curDrawingIndex); //当前笔的处理，可能要逐步画
                    Path path2 = this.curStrokePath;
                    if (path2 != null && paramCanvas != null) {
                        paramCanvas.drawPath(path2, this.strokePaint);
                    }
                    if (this.animateValue == 0.0F) {
                        this.maskRadius = 0.0F;
                    } else {
                        this.maskRadius = this.rectSize * 1.0F / 30;
                    }
                    PathMeasure pathMeasure = this.curDrawPathMeasure;  //用于修正起笔的圆角
                    if (pathMeasure == null) {
                    } else {
                        pathMeasure.getPosTan(0.0F, this.maskPos, this.maskTan);
                    }
                    if (paramCanvas != null) {
                        maskRadius = this.rectSize * 1.0F / 20;
                        paramCanvas.drawCircle(this.maskPos[0], this.maskPos[1], this.maskRadius, this.maskPaint);
                    }
                    Path path1 = this.curDrawPath; // todo 这里是动画的关键？？？
                    if (path1 != null && paramCanvas != null) {
                        paramCanvas.drawPath(path1, this.touchPaint); // 用的覆盖模式
                    }
                    if (paramCanvas != null) {
                        //paramCanvas.restoreToCount(k);
                    }
                }
                j = this.curDrawingIndex;  //i 始终为0， 画到当前笔，画黄色
                for (int b = i; b < j; b++) {
                    this.curStrokePath = (Path) this.strokePaths.get(b);
                    this.strokePaint.setColor(this.touchColor);
                    Path path = this.curStrokePath;
                    if (path != null && paramCanvas != null) {
                        paramCanvas.drawPath(path, this.strokePaint);
                    }
                }
            } else if (this.curDrawingIndex <= this.strokePaths.size()) {
                int k = this.curDrawingIndex + 1;  // 未写完的部分
                Log.d("progs", "****mid:" + this.showMedian + " bihua:" + this.curDrawingIndex + " all:" + this.strokePaths.size());
                i = this.strokePaths.size();
                while (k < i) {
                    this.curStrokePath = (Path) this.strokePaths.get(k);
                    this.strokePaint.setColor(showMedian ? this.strokeColor : this.touchColor);
                    Path path = this.curStrokePath;
                    if (path != null && paramCanvas != null) {
                        paramCanvas.drawPath(path, this.strokePaint);
                    }
                    k++;
                }
                if (this.curDrawingIndex < this.strokePaths.size()) {
                    if (paramCanvas != null) {
                        k = paramCanvas.saveLayer(0.0F, 0.0F, this.rectSize, this.rectSize, null, Canvas.ALL_SAVE_FLAG);
                    } else {
                        k = 0;
                    }
                    this.strokePaint.setColor(showMedian ? this.strokeColor : this.touchColor);
                    this.curStrokePath = (Path) this.strokePaths.get(this.curDrawingIndex);
                    Path path = this.curStrokePath;
                    if (path != null && paramCanvas != null) {
                        paramCanvas.drawPath(path, this.strokePaint);
                    }
                    if (this.showMedian) {
                        path = this.curMedianPath;
                        if (path != null) {
                            if (paramCanvas != null) {
                                paramCanvas.drawPath(path, this.medianPaint);
                            }
                            drawArrow(paramCanvas, path);
                        }
                    }
                    path = this.curDrawPath;
                    if (path != null && paramCanvas != null) {
                        paramCanvas.drawPath(path, this.touchPaint);
                    }
                    this.curStrokePath = (Path) this.strokePaths.get(this.curDrawingIndex);
                    this.curPathRectF.setEmpty();
                    path = this.curStrokePath;
                    if (path != null) {
                        path.computeBounds(this.curPathRectF, true);
                    }
                    this.curPathRegion.setEmpty();
                    this.curClipRegion.set((int) this.curPathRectF.left, (int) this.curPathRectF.top, (int) this.curPathRectF.right, (int) this.curPathRectF.bottom);
                    this.curPathRegion.setPath(this.curStrokePath, this.curClipRegion); //取交集， 应该就是笔画覆盖范围了
                    if (paramCanvas != null) {
                        paramCanvas.restoreToCount(k);
                    }
                }
                i = this.curDrawingIndex;
                for (k = j; k < i; k++) {
                    this.curStrokePath = (Path) this.strokePaths.get(k);
                    this.strokePaint.setColor(showMedian ? this.touchColor : this.strokeColor);
                    Path path = this.curStrokePath;
                    if (path != null && paramCanvas != null) {
                        paramCanvas.drawPath(path, this.strokePaint);
                    }
                }
            }
            if (!"".equals(pinyin)) {
                Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                textPaint.setTextSize(110);
                paramCanvas.drawText(pinyin, (this.rectSize - textPaint.measureText(pinyin)) / 2, 90, textPaint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int paramInt1, int paramInt2) {
        super.onMeasure(paramInt1, paramInt2);
        int im = View.MeasureSpec.getMode(paramInt2);
        int is = View.MeasureSpec.getSize(paramInt1);
        int jm = View.MeasureSpec.getMode(paramInt2);
        int js = View.MeasureSpec.getSize(paramInt2);
        if (js == Integer.MIN_VALUE) {
            js = is;
        }
        this.rectSize = Math.min(is, js);
        if (this.autoDraw) {
            this.touchWidth = this.rectSize * 1.0F / 10;
        } else {
            this.touchWidth = this.rectSize * 1.0F / 8;
        }
        this.maskRadius = this.rectSize * 1.0F / 30;
        this.touchPaint.setStrokeWidth(this.touchWidth);
        //todo log
        RectF rectF = this.gridRect;
        rectF.left = (this.gridWidth * 2);
        rectF.top = (this.gridWidth * 2);
        rectF.right = (this.rectSize - this.gridWidth * 2);
        rectF.bottom = (this.rectSize - this.gridWidth * 2);
        setMeasuredDimension(this.rectSize, this.rectSize);
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && this.curDrawingIndex < this.strokePaths.size()) {
            this.animateHandler.sendMessageDelayed(Message.obtain((Handler) this.animateHandler, 1), 100);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (autoDraw) {
            return super.onTouchEvent(event);
        }
        boolean bool = false;
        Path path1;
        this.lastX = this.downX;
        this.lastY = this.downY;
        this.downX = event.getX();
        this.downY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:


                if (this.curDrawPath != null) {
                    this.curDrawPath.reset();
                }
                if (inStartPointRange(this.downX, this.downY)) {
                    Log.d(this.TAG, "开始，在起点范围内");
                    this.curDrawingPathValid = true;
                    this.midDrawingValid = true;
                    this.curWrongTouchCount = 0;
                    if (this.curDrawPath != null) {
                        this.curDrawPath.moveTo(this.downX, this.downY);
                        return true;
                    }
                } else {
                    this.curDrawingPathValid = false;
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.curDrawingPathValid) {
                    if (this.midDrawingValid) {
                        if (!inMidPointRange(event.getX(), event.getY())) {
                            if (this.curWrongTouchCount < this.maxWrongTouch) {
                                this.curWrongTouchCount++;
                                Log.e("progsoft", "onTouchEvent: 滑动中，不在中线，但没超过中线阈值，当前阈值为" + this.curWrongTouchCount);
                            } else {
                                Log.d(this.TAG, "滑动中，不在中线，已超过阈值");
                                this.midDrawingValid = false;
                            }
                        }
                        bool = true;
                    } else {
                        Log.d(this.TAG, "滑动中，不在中线，忽略");
                    }
                } else {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.curDrawingPathValid && this.midDrawingValid && inEndPointRange(event.getX(), event.getY())) {
                    this.curDrawingIndex++;
                    Log.d(this.TAG, "结束，在末点范围内，开始下一划" + curDrawingIndex + "," +strokePaths.size());
                    if (this.curDrawingIndex > this.strokePaths.size()) {
                        this.curDrawingIndex = this.strokePaths.size() - 1;
                    } else {
                        if (this.curDrawPath != null) {
                            this.preDrawPaths.add(this.curDrawPath);
                            Log.d(this.TAG, "preDrawPaths.add");
                        }
                        if (this.curDrawPath != null) {
                            this.curDrawPath.reset();
                            Log.d(this.TAG, "curDrawPath.reset");
                        }
                        if (this.curDrawingIndex < this.strokePaths.size() && this.curDrawingIndex < this.medianPaths.size()) {
                            Log.d(this.TAG, "strokePaths.medianPaths");
                            this.curMedianPath = (Path) this.medianPaths.get(this.curDrawingIndex);
                            PathMeasure pathMeasure = this.curDrawPathMeasure; // 手写模式， 重写设置下一笔的中线 测量
                            if (pathMeasure != null) {
                                pathMeasure.setPath(this.curMedianPath, false);
                                Log.d(this.TAG, "pathMeasure.setPath");
                            }
                            StrokeDrawListener strokeDrawListener = this.strokeDrawCompleteListener;
                            if (strokeDrawListener != null) {
                                strokeDrawListener.onStrokeStartDraw(this.curDrawingIndex);
                                Log.d(this.TAG, "strokeDrawListener.onStrokeStartDraw");
                            }
                        } else {
                            StrokeDrawListener strokeDrawListener = this.strokeDrawCompleteListener;
                            if (strokeDrawListener != null) {
                                strokeDrawListener.onStrokeDrawComplete();
                                Log.d(this.TAG, "strokeDrawListener.onStrokeDrawComplete");
                            }
                        }
                        invalidate();
                    }
                } else {
                    Log.d(this.TAG, "结束，不在末点范围内，清除");
                    if (this.curDrawPath != null) {
                        this.curDrawPath.reset();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (this.curDrawPath != null) {
                    this.curDrawPath.reset();
                }
                break;
            default:
                break;
        }
        invalidate();
        this.midDrawingValid = bool;
        return true;
    }

    public final void redraw(Boolean paramBoolean) {
        this.preDrawPaths.clear();
        if (paramBoolean) {
            clear();
        }
        this.animateHandler.removeMessages(1);
        postDelayed(new ChineseCharacterView$redraw$1(), 300);
    }

    /**
     * 自动播放
     */
    private boolean autoDraw;
    public void setAutoDraw(boolean autoDraw) {
        this.autoDraw = autoDraw;
    }

    /**
     * 展示笔画
     */
    private boolean showMedian = false;
    public void setShowMedian(boolean showMedian) {
        this.showMedian = showMedian;
    }
    public final ChineseCharacterView setMedianOriPaths(List<String> paramList) {
        this.medianOriPaths = paramList;
        return this;
    }

    public final ChineseCharacterView setMedianPaths(String src) {
        this.pathPoints = parseMedianData(src);
        return this;
    }

    public final ChineseCharacterView setNeedShift(boolean paramBoolean) {
        this.needShift = paramBoolean;
        return this;
    }

    /**
     *
     * @param paramStrokeDrawListener x
     * @return 注册笔画的回调处理， 实际没有调用
     */
    public final ChineseCharacterView setStrokeDrawListener(StrokeDrawListener paramStrokeDrawListener) {
        this.strokeDrawCompleteListener = paramStrokeDrawListener;
        return this;
    }

    public final ChineseCharacterView setStrokeInfo(String src) {
        this.strokeInfo = parseStrokeData(src);
        return this;
    }

    public interface StrokeDrawListener {
        /**
         * 通知是否结束， 没有注册
         */
        void onStrokeDrawComplete();

        /**
         * 通知画几笔， 没有注册
         * @param param1Int
         */
        void onStrokeStartDraw(int param1Int);
    }

    class TimerHandler extends Handler {
        private final WeakReference<ChineseCharacterView> characterView;

        private final int gap = dp2px(4.0F);

        public TimerHandler(ChineseCharacterView param1ChineseCharacterView) {
            this.characterView = new WeakReference(param1ChineseCharacterView);
        }

        @Override
        public void handleMessage(Message param1Message) {
            WeakReference weakReference = this.characterView;
            if (weakReference != null) {
                ChineseCharacterView chineseCharacterView = (ChineseCharacterView) weakReference.get();
                if (chineseCharacterView != null) {
                    Log.e("progs", "boolean:" + chineseCharacterView.isPause + "," + chineseCharacterView.isStop + " " + chineseCharacterView.animateValue + "," + this.gap + "," + chineseCharacterView.curDrawingIndex);
                    if (!chineseCharacterView.isPause) {
                        if (chineseCharacterView.isStop) {
                            return;
                        }
                        if (chineseCharacterView.animateValue == 0.0F) {
                            ChineseCharacterView.StrokeDrawListener strokeDrawListener = chineseCharacterView.strokeDrawCompleteListener;
                            Log.e(TAG, "strokeDrawListener:" + strokeDrawListener);
                            if (strokeDrawListener != null) {
                                strokeDrawListener.onStrokeStartDraw(chineseCharacterView.curDrawingIndex);
                            }
                        }
                        chineseCharacterView.setAnimateValue(chineseCharacterView.animateValue + this.gap);
                        return;
                    }
                    return;
                }
            }
        }
    }

    /**
     * redraw Runnable
     */
    class ChineseCharacterView$redraw$1 implements Runnable {
        @Override
        public void run() {
            //没有绘制字词时， 不能演示否则程序奔溃
            if (strokeInfo == null)
                return;
            ChineseCharacterView.this.pathMatrix.reset(); //设置田字格大小，用于映射字体大小
            float f1;
            float f2;
            float f3 = (ChineseCharacterView.this.rectSize * 1.0F - ChineseCharacterView.this.fixedSize);  // 放大比例
            float f4, f5, f6, f7;
            if (ChineseCharacterView.this.needShift) {
                f1 = (ChineseCharacterView.this.rectSize - ChineseCharacterView.this.fixedSize) * 1.0F /2;
                f2 = (ChineseCharacterView.this.rectSize - ChineseCharacterView.this.fixedSize * ChineseCharacterView.this.shiftFactor) * 1.0F / 2; // 实际 0.8倍 原图
            } else {
                f1 = (ChineseCharacterView.this.rectSize - ChineseCharacterView.this.fixedSize) * 1.0F /2;
                f2 = f1;
            }
            f4 = f5 = ChineseCharacterView.this.rectSize / 2;
            f6 = f7 = 0;

            ChineseCharacterView.this.pathMatrix.postTranslate(f1, f2); // 平移（translate） 缩放（scale）  旋转（rotate）  错切（skew）
            f3 /= 1;

            if (ChineseCharacterView.this.needShift) {
                //ChineseCharacterView.this.pathMatrix.postScale(f3, -f3, (ChineseCharacterView.this.rectSize / 2), (ChineseCharacterView.this.rectSize / 2));
                ChineseCharacterView.this.pathMatrix.postScale(f3, -f3, f4 ,f5);
            } else {
                //ChineseCharacterView.this.pathMatrix.postScale(f3, f3, (ChineseCharacterView.this.rectSize / 2), (ChineseCharacterView.this.rectSize / 2));
                ChineseCharacterView.this.pathMatrix.postScale(f3, f3, f4 ,f5);
            }
            ChineseCharacterView.this.pathMatrix.postTranslate(f6, f7);
            Log.e(TAG, "rectSize:" + rectSize + "f:" + f1 + "," + f2 + "," + f3 + "," + f4 + "," + f5);
            if (strokePaths == null || strokePaths.size() == 0) {
                ChineseCharacterView.this.initStrokePaths();  // 重写初始化字体底图
            }
            if (medianPaths == null || medianPaths.size() == 0) {
                ChineseCharacterView.this.initMedianPaths();  // 重写初始化笔画图
            }
            if (!ChineseCharacterView.this.strokePaths.isEmpty()) {    // 变量上面初始化
                if (ChineseCharacterView.this.medianPaths.isEmpty()) { // 变量上面初始化
                    return;
                }
                ChineseCharacterView.this.curDrawingIndex = 0;
                if (ChineseCharacterView.this.autoDraw) {
                    ChineseCharacterView.this.curDrawPath = new Path();
                    ChineseCharacterView.this.curMedianPath = (Path) ChineseCharacterView.this.medianPaths.get(ChineseCharacterView.this.curDrawingIndex);
                    ChineseCharacterView.this.curDrawPathMeasure = new PathMeasure(ChineseCharacterView.this.curMedianPath, false); // 设置中线笔画 这里可能设置错误了
                    ChineseCharacterView.this.setAnimateValue(0.0F);
                    return;
                }
                ChineseCharacterView.this.curDrawPath = new Path();
                ChineseCharacterView.this.curMedianPath = (Path) ChineseCharacterView.this.medianPaths.get(ChineseCharacterView.this.curDrawingIndex);
                ChineseCharacterView.this.curDrawPathMeasure = new PathMeasure(ChineseCharacterView.this.curMedianPath, false);
                ChineseCharacterView.StrokeDrawListener strokeDrawListener = ChineseCharacterView.this.strokeDrawCompleteListener; // 显示中间指导笔画时 设置 中线笔画
                if (strokeDrawListener != null) {
                    strokeDrawListener.onStrokeStartDraw(ChineseCharacterView.this.curDrawingIndex);
                }
                ChineseCharacterView.this.invalidate();
            }
            return;
        }
    }

    private void initMedianPaths() {
        if (pathPoints != null) {
            medianPaths.clear();
            for (List<? extends PointF> pathPoint : pathPoints) { //多少笔画=多少循环
                if (pathPoint != null || pathPoint.size() != 0) {
                    Path path = new Path();
                    path.moveTo(pathPoint.get(0).x, pathPoint.get(0).y);
                    for (int i = 0; i < pathPoint.size() - 1; i++) { // 每一笔有多少个点阵
                        try {
                            // quadTo 用于绘制圆滑曲线， 即贝塞尔曲线
                            path.quadTo(pathPoint.get(i).x, pathPoint.get(i).y, pathPoint.get(i + 1).x, pathPoint.get(i + 1).y);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    path.transform(this.pathMatrix); //画线图，变换大小和镜像， 字体库获取的是上下镜像图
                    medianPaths.add(path);
                }
            }
        } else {
            Log.e(TAG, "******************这里永远跑不到******************");
            medianPaths.clear();
            for (String path : medianOriPaths) {
                Path path1 = parser(path);
                path1.transform(this.pathMatrix);  //画线图，变换大小和镜像， 字体库获取的是上下镜像图
                medianPaths.add(path1);

            }
        }
    }


    private List<List<PointF>> parseMedianData(String paramString) {
        boolean bool;
        Log.e(TAG, "medianString:" + paramString);
        CharSequence charSequence = (CharSequence) paramString;
        if (charSequence == null || charSequence.length() == 0) {
            bool = true;
        } else {
            bool = false;
        }
        if (bool) {
            return null;
        }
        List<List> stringList = JsonUtil.parseArray(paramString, List.class);
        List<List<PointF>> result = new ArrayList<>();
        for (List s : stringList) {
            List<List> list = JsonUtil.parseArray(s.toString(), List.class);
            List<PointF> pointFS = new ArrayList<>();
            for (List slll : list) {
                List<Number> numberList = JsonUtil.parseArray(slll.toString(), Number.class);
                PointF f = new PointF(numberList.get(0).floatValue(), numberList.get(1).floatValue());
                pointFS.add(f);
            }
            result.add(pointFS);
        }
        return result;
    }


    private List<String> parseOriMedianData(String paramString) {
        boolean bool;
        CharSequence charSequence = (CharSequence) paramString;
        if (charSequence == null || charSequence.length() == 0) {
            bool = true;
        } else {
            bool = false;
        }
        return bool ? null : JsonUtil.parseArray(paramString, String.class);
    }


    private List<String> parseStrokeData(String paramString) {
        boolean bool;
        List list;
        CharSequence charSequence = (CharSequence) paramString;
        if (charSequence == null || charSequence.length() == 0) {
            bool = true;
        } else {
            bool = false;
        }
        if (bool) {
            list = new ArrayList();
            return list;
        }
        return JsonUtil.parseArray(paramString, String.class);
    }

    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}