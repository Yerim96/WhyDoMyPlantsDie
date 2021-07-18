package kr.sswu.whydomyplantsdie.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

import kr.sswu.whydomyplantsdie.Model.ContentDTO;
import kr.sswu.whydomyplantsdie.R;
import kr.sswu.whydomyplantsdie.WritePostActivity;
import kr.sswu.whydomyplantsdie.databinding.ItemDetailPostBinding;

public class FeedFragment extends Fragment {

    private FirebaseUser user;
    private Button btn_addPost;

    public void DetailViewFragment() {
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.fragment_feed, container, false);

        btn_addPost = rootView.findViewById(R.id.btn_createPost);
        btn_addPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WritePostActivity.class);
                startActivity(intent);
            }
        });

        RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.feed_recyclerview);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(new DetailRecyclerViewAdapter());
        return rootView;
    }

    private class DetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private ArrayList<ContentDTO> contentDTOs;
        private ArrayList<String> contentUidList;

        DetailRecyclerViewAdapter(){
            contentDTOs = new ArrayList<>();
            contentUidList = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("feed").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    contentDTOs.clear();
                    contentUidList.clear();

                    for(DataSnapshot snapshot1 : snapshot.getChildren()){
                        contentDTOs.add(snapshot1.getValue(ContentDTO.class));
                        contentUidList.add(snapshot1.getKey());
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_post, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final int finalPosition = position;
            final ItemDetailPostBinding binding = ((CustomViewHolder) holder).getBinding();

            // 유저 이미지
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("profileImages").child(contentDTOs.get(position).uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {

                                @SuppressWarnings("VisibleForTests")
                                String url = dataSnapshot.getValue().toString();

                                Glide.with(holder.itemView.getContext())
                                        .load(url)
                                        .apply(new RequestOptions().circleCrop()).into(binding.itemdetailpostUserImgae);

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

            // 유저 아이디
            binding.itemdetailpostUserId.setText(contentDTOs.get(position).userId);

            // 식물 이미지
            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).imageUrl)
                    .placeholder(R.drawable.icon_loading)
                    .error(R.drawable.icon_close)
                    .into(binding.itemdetailpostPlantImage);

            //좋아요 이미지
            binding.itemdetailpostLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    likeEvent(finalPosition);
                }
            });
            if(contentDTOs.get(position).LIKES.containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                binding.itemdetailpostLike.setImageResource(R.drawable.icon_after_like);
            }
            else{
                binding.itemdetailpostLike.setImageResource(R.drawable.icon_before_like);
            }

            //좋아요 개수
            binding.itemdetailpostLikeCnt.setText("좋아요 " + contentDTOs.get(position).likeCount + "개");

            // 설명란 유저 아이 텍스트
            String name[] = contentDTOs.get(position).userId.split("@");
            binding.itemdetailpostContentUserid.setText(name[0]);

            // 설명 텍스트
            binding.itemdetailpostContent.setText(contentDTOs.get(position).explain);

        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }

        private void likeEvent(int position){
            final int finalPosition = position;
            FirebaseDatabase.getInstance().getReference("feed").child(contentUidList.get(position))
                    .runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            ContentDTO contentDTO = currentData.getValue(ContentDTO.class);
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            if(contentDTO == null){
                                return Transaction.success(currentData);
                            }
                            if(contentDTO.LIKES.containsKey(uid)){
                                contentDTO.likeCount = contentDTO.likeCount - 1;
                                contentDTO.LIKES.remove(uid);
                            }
                            else{
                                contentDTO.likeCount = contentDTO.likeCount + 1;
                                contentDTO.LIKES.put(uid, true);

                            }
                            currentData.setValue(contentDTO);
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                        }
                    });
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            //data binding
            private ItemDetailPostBinding binding;

            CustomViewHolder(View itemView) {
                super(itemView);
                binding = DataBindingUtil.bind(itemView); //error 발생시 Invalidate Cashes/Restart 실행
            }

            ItemDetailPostBinding getBinding() {
                return binding;
            }
        }
    }
}

