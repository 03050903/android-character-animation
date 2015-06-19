package com.stone.textview.animation;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

@SuppressLint("HandlerLeak")
public class AnimTextView extends View {

	private String showText;
	private Paint paint;
	private List<CharHolder> charList = new ArrayList<CharHolder>();
	private int cycleNum = 0; // �߳�ѭ����sleep�Ĵ���
	private int maxDelayNum = 16; // ÿ������delay���߳�ѭ������������ģ����ֵ������������ֵ������
	private int finishCycleNum = 8; // ��͸������ȫ��͸�����߳�ѭ������

	// ��onDraw�����е���canvas.drawText��Ҫ����x/y���꣬�Ǹ��������������½ǵ����ꡣ
	private int firstLineOffset = 0;

	// drawText��y��ʼֵ�ǳ����ܣ�����stackoverflow�ķ������Ǹ�yֵҪͨ�����㵥������ռ�ݵĸ߶ȣ��űȽϿ�ѧ���������Ǵ���һ����ƫ��
	// ���extraPaddingTop���������ֲ�ƫ��ģ������������ȷ
	private int extraPaddingTop = PixValue.dip.valueOf(1f);

	// ����������Ҫͨ��xml�ļ�������
	private int textSize = PixValue.sp.valueOf(14); // �ֺ�
	private int textColor = Color.WHITE; // ������ɫ
	private int lineSpaceExtra = 10; // �о�

	public AnimTextView(Context context) {
		this(context, null);
	}

	public AnimTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnimTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		// 1. ��ʼ����ʾ�ı����ֺš���ɫ���о����Ϣ
		TypedArray typedArray = context.obtainStyledAttributes(attrs,
				R.styleable.textAnim);
		showText = typedArray.getString(R.styleable.textAnim_showText);
		textSize = typedArray.getDimensionPixelSize(
				R.styleable.textAnim_textSize, textSize);
		textColor = typedArray.getColor(R.styleable.textAnim_textColor,
				textColor);
		lineSpaceExtra = typedArray.getDimensionPixelSize(
				R.styleable.textAnim_lineSpaceExtra, lineSpaceExtra);
		typedArray.recycle();

		// 2. ��ʼ��paint����
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(textColor);
		paint.setTextSize(textSize);
		paint.setTextAlign(Align.LEFT);

		// 3. ��ʼ���Ǹ����ܵ�y����ֵ
		Rect rect = new Rect();
		paint.getTextBounds("��", 0, 1, rect); // ��һ�����͵ĺ���Ϊģ�壬����߶�
		firstLineOffset = (int) (rect.height() - rect.bottom) + extraPaddingTop; // stackoverflow������˸��Ľ���

		// 3. ��ʼ���ַ���
		initCharList();
	}

	private void initCharList() {
		// �����ʾ�ı�Ϊ�գ�����ʾ
		if (showText == null || showText.length() == 0) {
			return;
		}

		charList.clear();

		int length = showText.length();
		for (int i = 0; i < length; i++) {
			CharHolder charItem = new CharHolder();
			charItem.charTxt = "" + showText.charAt(i);

			if ("\n".equals(charItem.charTxt)) {
				// ���С�����Ҫalpha����
				charItem.initDelay = Integer.MAX_VALUE;
				charItem.measureWidth = 0;
				charList.add(charItem);
				continue;
			}

			charItem.measureWidth = (int) paint.measureText(charItem.charTxt);

			if (" ".equals(charItem.charTxt)) {
				// �ո���Ҫalpha����
				charItem.initDelay = Integer.MAX_VALUE;
			} else {
				charItem.initDelay = (int) (Math.random() * maxDelayNum);
			}
			charList.add(charItem);
		}
	}

	@Override
	protected void onFinishInflate() {
		new UIThread().start();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int fromX = getPaddingLeft();
		int fromY = getPaddingTop() + firstLineOffset; // ���y����ܶ��ģ���ת��firstLineOffset����

		// ��������װ�����ֵ������
		int maxLineWidth = getWidth() - getPaddingLeft() - getPaddingRight();

		// ��ʱ��ÿһ�еĿ�ȣ���̬�ı�
		int thisLineWidth = 0;
		for (CharHolder itemHolder : charList) {

			// 1. ����drawText��x/yλ��
			String drawChar = itemHolder.charTxt;
			thisLineWidth = thisLineWidth + itemHolder.measureWidth; // �������߶�ʮһ���ȼ�����������˵

			if (drawChar.equals("\n")) {
				// ��������
				fromY = fromY + textSize + lineSpaceExtra;
				fromX = getPaddingLeft();
				thisLineWidth = 0;
				continue;
			} else if (thisLineWidth > maxLineWidth) {
				// ��һ�����أ�װ������
				fromY = fromY + textSize + lineSpaceExtra;
				fromX = getPaddingLeft();
				thisLineWidth = itemHolder.measureWidth; // ��ע�������ʱ��Ȳ���0
			}

			// 2. ����alphaֵ
			int alpha = 0;
			int delayInterval = cycleNum - itemHolder.initDelay;
			if (delayInterval > finishCycleNum) {
				alpha = 255;
			} else if (delayInterval > 0) {
				alpha = 255 * (cycleNum - itemHolder.initDelay)
						/ finishCycleNum;
			}

			// 3. ����drawText�������
			if (alpha > 0) {
				// alpha�������ʱ���drawText����ʡ��Դ��
				paint.setAlpha(alpha);
				canvas.drawText(drawChar, fromX, fromY, paint);
			}

			fromX += itemHolder.measureWidth;
		}

		paint.setAlpha(255);

	}

	// ����װ�ع�����
	class CharHolder {
		String charTxt; // ��������
		int measureWidth = 0; // ���ֵĿ��
		int initDelay; // ����delay��ʾ�Ĵ���
	}

	class UIThread extends Thread {

		public UIThread() {
			cycleNum = 0;
		}

		@Override
		public void run() {
			int maxCyleNum = finishCycleNum + maxDelayNum;

			try {
				// ˯400���룬�Ӿ�����
				sleep(400);

				while (cycleNum < maxCyleNum) {
					sleep(60);

					// handler֪ͨui��������͸����
					Message msg = uiHandler.obtainMessage();
					cycleNum++;
					msg.sendToTarget();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		};
	};

	private Handler uiHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// ˢ��View
			invalidate();
		};
	};

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		// ��дonMeasure������û����View���ֶ�����߶�
		setMeasuredDimension(getMeasuredWidth(),
				getMeasureHeight(heightMeasureSpec));
	}

	/**
	 * ������View�ĸ߶�
	 */
	private int getMeasureHeight(int heightMeasureSpec) {
		int result = 0;
		int size = MeasureSpec.getSize(heightMeasureSpec);
		int mode = MeasureSpec.getMode(heightMeasureSpec);
		if (mode == MeasureSpec.EXACTLY) {
			// �����ֱ��д���ˣ�����xml�ļ���д����android:layout_height="200dp"
			result = size;
		} else {
			result = computeViewHeight();
		}
		return result;
	}

	/**
	 * �������ֺ��ֺŲ����߶�
	 */
	private int computeViewHeight() {
		int widgetHeight = getPaddingTop() + getPaddingBottom() + textSize;
		int maxTextWidth = getMeasuredWidth() - getPaddingLeft()
				- getPaddingRight();
		int thisLineWidth = 0;
		for (CharHolder txtHolder : charList) {
			String charTxt = txtHolder.charTxt;
			if ("\n".equals(charTxt)) {
				// �ַ���������������
				thisLineWidth = 0;
				widgetHeight = widgetHeight + lineSpaceExtra + textSize;
				continue;
			}

			thisLineWidth += txtHolder.measureWidth;
			if (thisLineWidth > maxTextWidth) {
				// ��ȼ��㳬���߽�
				thisLineWidth = txtHolder.measureWidth;
				widgetHeight = widgetHeight + lineSpaceExtra + textSize;
			}
		}
		return widgetHeight;
	}

	public String getShowText() {
		return showText;
	}

	public void setShowText(String showText) {
		this.showText = showText;

		initCharList();
		requestLayout();

		new UIThread().start();
	}
}
