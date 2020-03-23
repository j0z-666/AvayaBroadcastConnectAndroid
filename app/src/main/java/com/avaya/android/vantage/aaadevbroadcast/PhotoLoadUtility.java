package com.avaya.android.vantage.aaadevbroadcast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.avaya.android.vantage.aaadevbroadcast.model.ContactData;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;

import java.util.Objects;

/**
 * Class used to load and cache photos
 */
public class PhotoLoadUtility {

    private static final String TAG = PhotoLoadUtility.class.getSimpleName();

    static public void setThumbnail(@NonNull ContactData contactData, final ImageView photo) {
        if ((photo == null) || (photo.getContext() == null)) {
            return;
        }

        if (contactData.mCategory == ContactData.Category.IPO || contactData.mCategory == ContactData.Category.BROADSOFT) {
            return;
        }

        Context context = ElanApplication.getContext();
        assert context != null;
        Glide.with(photo.getContext()).clear(photo);

        if (contactData.mPhotoURI != null && contactData.mPhotoThumbnailURI != null && contactData.mPhotoThumbnailURI.length() > 0) {
            Glide.with(photo.getContext())
                    .asBitmap()
                    .apply(new RequestOptions().signature(new ObjectKey(contactData.mPhotoURI)).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop())
                    .load(contactData.mPhotoThumbnailURI)
                    .into(new ViewTarget<ImageView, Bitmap>(photo) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                            ImageView photoView = this.view;
                            if (photoView == null) {
                                return;
                            }
                            // making bitmap roundable
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(photoView.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            photoView.setBackground(circularBitmapDrawable);
                        }
                    });
        } else if (contactData.mPhoto != null && contactData.mPhoto.length > 0) {
            PhotoLoadUtility.setPhoto(contactData, photo);
        } else {
            photo.setBackground(context.getDrawable(R.drawable.ic_avatar_generic));
        }
    }

    static public void setThumbnail(@NonNull ContactData contactData, final TextView photo, boolean firstNameFirst) {
        if ((photo == null) || (photo.getContext() == null)) {
            return;
        }

        if (contactData.mCategory == ContactData.Category.IPO || contactData.mCategory == ContactData.Category.BROADSOFT) {
            PhotoLoadUtility.setInitials(contactData, photo, firstNameFirst);
            return;
        }

        Glide.with(photo.getContext()).clear(photo);
        Context context = ElanApplication.getContext();
        assert context != null;
        int width = context.getResources().getInteger(R.integer.contact_photo_width);
        int height = context.getResources().getInteger(R.integer.contact_photo_width);

        if (contactData.mPhotoURI != null && contactData.mPhotoThumbnailURI != null && contactData.mPhotoThumbnailURI.length() > 0) {
            photo.setText("");
            Glide.with(photo.getContext())
                    .asBitmap()
                    .apply(new RequestOptions().signature(new ObjectKey(contactData.mPhotoURI)).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop())
                    .load(contactData.mPhotoThumbnailURI)
                    .into(new ViewTarget<TextView, Bitmap>(photo) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                            TextView photoView = this.view;
                            if (photoView == null) {
                                return;
                            }
                            // making bitmap roundable
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(photoView.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            photoView.setBackground(circularBitmapDrawable);
                        }
                    });
        } else if (contactData.mPhoto != null && contactData.mPhoto.length > 0) {
            photo.setText("");
            PhotoLoadUtility.setPhoto(contactData, photo, width, height);
        } else {
            PhotoLoadUtility.setInitials(contactData, photo, firstNameFirst);
        }
    }

    /**
     * Setting up circle photo
     *
     * @param contactData {@link ContactData}
     * @param photo       {@link View} in which photo have to be set
     */
    static public void setPhoto(@NonNull ContactData contactData, final ImageView photo) {
        Glide.with(photo.getContext())
                .asBitmap()
                .apply(new RequestOptions().dontAnimate().circleCrop())
                .load(contactData.mPhoto)
                .into(photo);
    }

    /**
     * Setting up circle photo
     *
     * @param contactData {@link ContactData}
     * @param photo       {@link View} in which photo have to be set
     * @param width       of photo
     * @param heigth      of photo
     */
    static private void setPhoto(@NonNull ContactData contactData, final View photo, int width, int heigth) {
        Glide.with(photo.getContext()).clear(photo);
        Glide.with(photo.getContext())
                .asBitmap()
                .apply(new RequestOptions().signature(new ObjectKey(contactData.mPhotoURI)).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop().dontAnimate())
                .load(contactData.mPhoto)
                .into(new SimpleTarget<Bitmap>(width, heigth) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(photo.getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        if (photo.getBackground() != circularBitmapDrawable) {
                            photo.setBackground(circularBitmapDrawable);
                        }
                    }
                });
    }

    static private void setInitials(ContactData contactData, final TextView photo, boolean firstNameFirst) {
        String name = contactData.mName;

        int colors[] = photo.getResources().getIntArray(R.array.material_colors);
        photo.setBackground(Objects.requireNonNull(ElanApplication.getContext()).getDrawable(R.drawable.empty_circle));
        ((GradientDrawable) photo.getBackground().mutate()).setColor(colors[Math.abs(name.hashCode() % colors.length)]);

        if (TextUtils.isEmpty(name)) {
            photo.setText("");
            return;
        }
        if (Patterns.PHONE.matcher(name).matches() && (contactData.mLastName == null || contactData.mLastName.isEmpty())) {
            photo.setText("#");
            return;
        }

        String initials;
        String firstNameLetter;
        String lastNameLetter;

        if (contactData.mFirstName != null && contactData.mFirstName.trim().length() > 0) {
            firstNameLetter = String.valueOf(contactData.mFirstName.trim().charAt(0));
        } else {
            firstNameLetter = "";
        }

        if (contactData.mLastName != null && contactData.mLastName.trim().length() > 0) {
            lastNameLetter = String.valueOf(contactData.mLastName.trim().charAt(0));
        } else {
            lastNameLetter = "";
        }

        if (firstNameFirst) {
            initials = firstNameLetter + lastNameLetter;
        } else {
            initials = lastNameLetter + firstNameLetter;
        }

        photo.setText(initials.toUpperCase());
    }
}