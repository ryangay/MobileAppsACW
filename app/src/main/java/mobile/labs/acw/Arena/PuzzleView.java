package mobile.labs.acw.Arena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.constraint.ConstraintLayout;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobile.labs.acw.Util;

/**
 * Created by ryan on 08/03/2018.
 */

public class PuzzleView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final String TAG = PuzzleView.class.getName();

    private static final int MARGIN = 2;
    private static final int DEFAULT_BACKGROUND_COLOUR = Color.CYAN;

    private static final Object mLock = new Object();

    private final Paint mPaint =  new Paint();
    private int mBackgroundColour = DEFAULT_BACKGROUND_COLOUR;
    private PuzzleGridDrawable[] mPieces;
    private int mRows;
    private int mColumns;
    private int mImageSize;
    private int mPieceSize;

    private Map<Vector2, Rect> cells;
    private final Map<PuzzleGridDrawable, Rect> mMovingPiecePositionMap = new IdentityHashMap<>(3);

    private int mInitialHeight;
    private int mInitialWidth;

    private PuzzleGridDrawable mTouched;
    private List<PuzzleGridDrawable> mPushed;

    private Thread mUpdateThread;
    private PuzzleViewCallback mCallback;

    private boolean mCanvasLocked;

    public PuzzleView(Context context) {
        this(context, null);
    }

    public PuzzleView(Context context, AttributeSet attrs){
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void setDimensions(int rows, int columns, int availableWidth, int availableHeight){
        mRows = rows;
        mColumns = columns;
        cells = new HashMap<>(mColumns*mRows);

        mInitialHeight = availableHeight;
        mInitialWidth = availableWidth;
        int width, height;
        // Setup the size of the view
        // Reference: https://stackoverflow.com/questions/11853297/change-size-of-android-custom-surfaceview
        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int containedSize;
        int pieceSize;
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
            // Find a width that is suitable
            containedSize = (availableWidth - (mColumns - 1)*MARGIN);
            boolean suitable = containedSize % mColumns == 0;
            for(width = availableWidth; !suitable; width--){
                containedSize = (width - (mColumns - 1)*MARGIN);
                suitable = containedSize % mColumns == 0;
            }
            if(mRows == mColumns){
                height = width;
            } else {
                height = mRows * (width / mColumns);
            }
            pieceSize = containedSize / mColumns;
        } else {
            // Find a height that is suitable
            containedSize = (availableHeight - (mRows - 1)*MARGIN);
            boolean suitable = containedSize % mRows == 0;
            for(height = availableHeight; !suitable; height--){
                containedSize = (height - (mRows - 1)*MARGIN);
                suitable = containedSize % mRows == 0;
            }
            width = mColumns*(height / mRows);
            pieceSize = containedSize / mRows;
        }

        for(int y = 0; y < mRows; y++){
            int top = y*(pieceSize + (y == 0 ? 0 : MARGIN));
            for (int x = 0; x < mColumns; x++){
                int left = x*(pieceSize + (x == 0 ? 0 : MARGIN));
                cells.put(new Vector2(x, y), new Rect(left, top, left+pieceSize, top+pieceSize));
            }
        }
        mPieceSize = pieceSize;
        setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    public void setPieces(PuzzleGridDrawable[] pieces, int imageSize){
        mPieces = pieces;
        mImageSize = imageSize;
        synchronized (mLock){
            mLock.notify();
        }
    }

    /**
     * Sets the background colour of the puzzle (mostly visible in the empty block)
     * @param red A red value between 0..255
     * @param green A green value between 0..255
     * @param blue A blue value between 0..255
     */
    public void setBackgroundColour(int red, int green, int blue){
        if(
                0 <= red   && red   <= 255 &&
                0 <= green && green <= 255 &&
                0 <= blue  && blue  <= 255
                ) {
            mBackgroundColour = Color.rgb(red, green, blue);
        } else {
            Log.d(TAG, "Tried to set out of bounds colour");
            throw new IllegalArgumentException("All arguments must be an integer between 0 and 255 inclusive.");
        }
    }

    public void start(){
        mUpdateThread.start();
    }

    public void stop(){
        mUpdateThread.interrupt();
    }

    public void setCallback(PuzzleViewCallback callback){
        mCallback = callback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mUpdateThread = new Thread(this);
        mCallback.onSetupFinished();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mInitialHeight = height;
        mInitialWidth = width;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mUpdateThread.interrupt();
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try{
                synchronized (mLock){
                    while(mPieces == null){
                        mLock.wait();
                    }
                }
            } catch (InterruptedException e){
                Log.e(TAG, "PuzzleView thread has been interrupted.", e);
                return;
            }
            Canvas canvas = null;
            if(!getHolder().getSurface().isValid()) continue;
            synchronized (getHolder()) {
                try {
                    /* THIS CAUSES A CRASH ON THE EMULATOR IN SOME UNKNOWN CIRCUMSTANCE
                     * DUE TO A BUG IN EITHER:
                     *  1. THE ANDROID SYSTEM'S FRAMEWORKS/NATIVE/LIB/GUI/INCLUDE/SURFACE.H native C code
                     *  2. SOME INCOMPATIBILITY BETWEEN THE ANDROID EMULATOR (QEMU) AND THE WINDOWS OPENGL DRIVER
                     *
                     *  ONLY KNOWN FIX IS TO DISABLE HARDWARE GRAPHICS RENDERING IN THE EMULATOR CONFIGURATION
                     */
                    if (!mCanvasLocked) {
                        canvas = getHolder().lockCanvas();
                        synchronized (this) {
                            mCanvasLocked = true;
                        }
                    }
                    if (canvas == null) {
                        mCanvasLocked = false;
                        continue;
                    }
                    canvas.drawColor(mBackgroundColour);
                    Paint paint = new Paint();
                    for (PuzzleGridDrawable piece : mPieces) {
                        Rect drawArea = null;
                        if (piece.mIsMoving) {
                            drawArea = mMovingPiecePositionMap.get(piece);
                        }
                        if (drawArea == null) {
                            drawArea = cells.get(piece.pos);
                        }
                        canvas.drawBitmap(piece.image, null, drawArea, paint);
                    }
                } catch (IllegalArgumentException e) {
                    // This usually happens when the canvas cannot be locked.
                    Log.e(TAG, "Exception drawing PuzzleView", e);
                } catch (Exception e) {
                    Log.e(TAG, "Exception drawing PuzzleView", e);
                } finally {
                    if (canvas != null && mCanvasLocked) {
                        getHolder().unlockCanvasAndPost(canvas);
                        synchronized (this) {
                            mCanvasLocked = false;
                        }
                    }
                }
            }
        }
    }



    /**
     * Acts on touches within the View
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        /*
        * Pieces should only be able to move in one direction,
        * x or y.
        * They should not be able to move if there is no free space within
        * the column or row that they occupy
        * When a piece is dragged then let go, 60% of its displacement should have crossed
        * the boundary of the next cell for it to change position, otherwise its position will revert
        * to the cell held before the drag action
        */


        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                for(PuzzleGridDrawable piece : mPieces){
                    if(hitTest(piece, (int)event.getX(), (int)event.getY(), null)){
                        mTouched = piece;
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                Rect pos = new Rect();
                int offsetX = 0, offsetY = 0;
                if (!hitTest(mTouched, (int) event.getX(), (int) event.getY(), pos)) break;
                if (!mTouched.isMoving()) {
                    MovementDirection dir = movable(mTouched);
                    if (dir != null) {
                        for (PuzzleGridDrawable piece : mPushed) {
                            piece.isMoving(true);
                            piece.setMoveDirection(dir);
                            mMovingPiecePositionMap.put(piece, new Rect(cells.get(piece.pos)));
                        }
                    }
                    break;
                } else {
                    Rect cellPos = cells.get(mTouched.pos);
                    switch (mTouched.mMovingDirection) {
                        case LEFT: {
                            int distance = (int) event.getX() - cellPos.centerX();
                            if (-mPieceSize <= distance && distance <= 0) {
                                offsetX = (int) event.getX() - pos.centerX();
                            }
                            break;
                        }
                        case RIGHT: {
                            int distance = (int) event.getX() - cells.get(mTouched.pos).centerX();
                            if (0 <= distance && distance <= mPieceSize) {
                                offsetX = (int) event.getX() - pos.centerX();
                            }
                            break;
                        }
                        case UP: {
                            int distance = (int) event.getY() - cellPos.centerY();
                            if (-mPieceSize <= distance && distance <= 0) {
                                offsetY = (int) event.getY() - pos.centerY();
                            }
                            break;
                        }
                        case DOWN: {
                            int distance = (int) event.getY() - cells.get(mTouched.pos).centerY();
                            if (0 <= distance && distance <= mPieceSize) {
                                offsetY = (int) event.getY() - pos.centerY();
                            }
                            break;
                        }
                    }
                }
                for (PuzzleGridDrawable piece : mPushed) {
                    Rect position = mMovingPiecePositionMap.get(piece);
                    position.offset(offsetX, offsetY);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                // Determine whether the piece has moved far enough for it to move cell,
                // or if the interaction was a tap, immediately move the piece into the space (if it exists)
                if(mTouched == null){
                    // Nothing has been touched
                    break;
                }
                MovementDirection dir;
                if (mTouched.mMovingDirection == null) dir = movable(mTouched);
                else dir = mTouched.mMovingDirection;
                if(dir != null) {
                    Map<PuzzleGridDrawable, Vector2> newPositions = new HashMap<>(mPushed.size());
                    boolean canShift = true;
                    for (PuzzleGridDrawable piece : mPushed) {
                        // The piece can be moved
                        switch (dir) {
                            case LEFT:
                                newPositions.put(piece, new Vector2(piece.pos.x - 1, piece.pos.y));
                                break;
                            case UP:
                                newPositions.put(piece, new Vector2(piece.pos.x, piece.pos.y - 1));
                                break;
                            case RIGHT:
                                newPositions.put(piece, new Vector2(piece.pos.x + 1, piece.pos.y));
                                break;
                            case DOWN:
                                newPositions.put(piece, new Vector2(piece.pos.x, piece.pos.y + 1));
                                break;
                        }
                        Vector2 newPosition = newPositions.get(piece);
                        if (newPosition == null || !(0 <= newPosition.x && newPosition.x < mColumns
                                && 0 <= newPosition.y && newPosition.y < mRows)) {
                            canShift = false;
                            break;
                        }
                    }

                    /* Hacky way to work round instances where the direction
                    ** makes the cell move OOB
                    ** I can't find the source of the problem right now
                    */

                    if(canShift) {
                        boolean shifted = false;
                        for(PuzzleGridDrawable piece : mPushed) {
                            Vector2 nextCell = newPositions.get(piece);
                            if (!piece.isMoving()) {
                                // If piece is tapped
                                piece.setPos(nextCell);
                                shifted = true;
                            } else if (locationIsInCell(mMovingPiecePositionMap.get(piece), nextCell)) {
                                // If piece is dragged
                                piece.setPos(nextCell);
                                shifted = true;
                            }
                            piece.setMoveDirection(null);
                            piece.isMoving(false);
                            mMovingPiecePositionMap.remove(piece);
                        }
                        if(shifted) mCallback.onMove();
                    }
                }

                mTouched = null;
                boolean finished = true;
                for (PuzzleGridDrawable piece : mPieces) {
                    finished = piece.isInCorrectPosition() && finished;
                }
                if (finished) {
                    mCallback.onFinish();
                }
                break;
        }
        return true;
    }

    MovementDirection movable(PuzzleGridDrawable piece){
        // Check that any adjacent cell is free
        // An adjacent cell is one that is directly above, below or to either side

        final Vector2 LEFT = new Vector2(piece.pos.x-1, piece.pos.y); // Left
        final Vector2 UP = new Vector2(piece.pos.x, piece.pos.y-1); // Up
        final Vector2 RIGHT = new Vector2(piece.pos.x+1, piece.pos.y); // Right
        final Vector2 DOWN = new Vector2(piece.pos.x, piece.pos.y+1);  // Down

        Set<MovementDirection> adjacents = new HashSet<>();
        if(piece.pos.x != mColumns - 1) {
            adjacents.add(MovementDirection.RIGHT);
        }
        if(piece.pos.x != 0){
            adjacents.add(MovementDirection.LEFT);
        }
        if(piece.pos.y != 0){
            adjacents.add(MovementDirection.UP);
        }
        if(piece.pos.y != mRows - 1){
            adjacents.add(MovementDirection.DOWN);
        }

        MovementDirection legalMove = null;
        for(MovementDirection adj : adjacents){
            mPushed = new ArrayList<>(Math.max(mRows, mColumns));
            if(collectPiecesInFrontOfSpace(mPushed, piece, adj)) {
                legalMove = adj;
                break;
            }
        }
        return legalMove;
    }

    boolean collectPiecesInFrontOfSpace(List<PuzzleGridDrawable> collect, PuzzleGridDrawable current, MovementDirection traversalDir){
        collect.add(current);

        Vector2 nextSpace = null;
        switch(traversalDir){

            case LEFT:
                nextSpace = new Vector2(current.pos.x - 1, current.pos.y);
                if(nextSpace.x < 0) return false;
                break;
            case RIGHT:
                nextSpace = new Vector2(current.pos.x + 1, current.pos.y);
                if(nextSpace.x >= mColumns) return false;
                break;
            case UP:
                nextSpace = new Vector2(current.pos.x, current.pos.y - 1);
                if(nextSpace.y < 0) return false;
                break;
            case DOWN:
                nextSpace = new Vector2(current.pos.x, current.pos.y + 1);
                if(nextSpace.y >= mRows) return false;
                break;
        }

        for(PuzzleGridDrawable piece : mPieces){
            if(piece.pos.equals(nextSpace)){
                return collectPiecesInFrontOfSpace(collect, piece, traversalDir);
            }
        }
        return true;
    }

    boolean hitTest(PuzzleGridDrawable piece, int x, int y, Rect outPos){
        Rect pos;
        if(piece == null) return false;
        if(piece.isMoving()) {
            pos = mMovingPiecePositionMap.get(piece);
        } else {
            pos = cells.get(piece.pos);
        }
        if(outPos != null){
            outPos.set(pos);
        }
        return pos.contains(x, y);
    }

    boolean locationIsInCell(Rect floatingLoc, Vector2 cell){
        if(floatingLoc == null || cell == null){
            return false;
        }
        if(floatingLoc.width() != mPieceSize || floatingLoc.height() != mPieceSize){
            // Check that the containing rectangle is the correct size
            return false;
        }
        Rect cellLoc = cells.get(cell);
        if(cellLoc == null){
            return false;
        }
        Rect intersect = new Rect();
        if(!intersect.setIntersect(floatingLoc, cellLoc)){
            return false;
        }
        final int threeQuarters = (mPieceSize*3)/5;
        if(cellLoc.left == floatingLoc.left && cellLoc.right == floatingLoc.right){
            // Moving up or down
            return intersect.height() >= threeQuarters;
        } else if(cellLoc.top == floatingLoc.top && cellLoc.bottom == floatingLoc.bottom){
            // Moving left or right
            return intersect.width() >= threeQuarters;
        }
        // Something's gone wrong (moving diagonally)
        return false;
    }

    public static class PuzzleGridDrawable{
        public final Bitmap image;
        public final Vector2 pos;
        private final Vector2 finalPos;

        private boolean mIsMoving;
        private MovementDirection mMovingDirection;

        public PuzzleGridDrawable(Bitmap image, int startPosX, int startPosY, int finalPosX, int finalPosY){
            this.image = image;
            this.pos = new Vector2(startPosX, startPosY);
            this.finalPos = new Vector2(finalPosX, finalPosY);
        }

        void setPos(Vector2 pos){
            this.pos.x = pos.x;
            this.pos.y = pos.y;
        }

        void isMoving(boolean moving) {
            mIsMoving = moving;
        }

        boolean isMoving() {
            return mIsMoving;
        }

        void setMoveDirection(MovementDirection dir){
            mMovingDirection = dir;
        }

        boolean isInCorrectPosition(){
            return pos.equals(finalPos);
        }
    }

    public interface PuzzleViewCallback{
        void onSetupFinished();
        void onMove();
        void onFinish();
    }

    private enum MovementDirection{
        LEFT, RIGHT, UP, DOWN
    }

    static class Vector2{
        public int x;
        public int y;

        public Vector2(int x, int y){
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Vector2){
                Vector2 other = (Vector2)obj;
                return other.x == x && other.y == y;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int code = x * 7;
            return y*35+code;
        }
    }
}
