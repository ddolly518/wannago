import DefaultLayout from '../layouts/DefatulLayout';
import PostForm from '../components/PostForm';
import DailyScheduleForm from '../components/DailyScheduleForm';
import Button from '../components/Button';
import '../assets/css/post-write.css';
import { useState } from 'react';
import { inputNewPost } from '../service/post-service';
import { Navigate, useNavigate } from 'react-router';
import { HttpStatusCode } from 'axios';

function PostWritePage () {
  const navigate = useNavigate();
  const [newPost, setNewPost] = useState(
      {
        title : '',
        contents : '',
        tags : [],
        schedules : [
          {
            day: '',
            timeSchedules : [
                {
                  title : '',
                  time : '12:00',
                  contents : '',
                  locationName: '',
                  lat: 0,
                  lng: 0,
                }
            ]
          },
        ]
      }
  );

  const completeWirtePost = () => {
    console.log("작성 완료했대!!");
    const schedules = newPost.schedules.map((schedule, idx) => {return {...schedule, day: `${idx+1}일차`}});
    inputNewPost({...newPost, schedules}).then((result) => {
      if(result.status === HttpStatusCode.Ok) {
        console.log('게시글 작성이 완료되었습니다.');
        const id = result.data;
        navigate(`/post/${id}`);
      } else {
        console.log('게시글 작성에 실패했습니다.');
      }
    }).catch((err) => {
      console.log('게시글 작성에 실패했습니다.');
      console.log(err);
    });;
  }

  const updateSchedule = (preIdx, newSchedule) => {
    const schedules = newPost.schedules.map((schedule, idx) => idx === preIdx ? newSchedule : schedule);
    setNewPost((prePost) => {return {...prePost, schedules : [...schedules]}});
    console.log(newPost.schedules[preIdx]);
  }

  const updateTItle = (title) => {
    setNewPost((prePost) => {return {...prePost, title}});
  }

    const updateContents = (contents) => {
    setNewPost((prePost) => {return {...prePost, contents}});
  }

  const updateTags = (tags) => {
    setNewPost((prePost) =>  {return {...prePost, tags}});
  }

  const addSchedule = () => {
    setNewPost((prePost) => {
      return {
        ...prePost,
        schedules: [
          ...prePost.schedules,
          {
            day: '',
            timeSchedules : [
                {
                  title : '',
                  time : '',
                  contents : '',
                  locationName: '',
                  lat: 0,
                  lng: 0,
                }
            ]
          },
        ]      
      }
    });
    console.log(newPost);
  }

  return (
    <DefaultLayout>
        <div className="body">
          <PostForm post={newPost} updateTitle={updateTItle} updateTags={updateTags} updateContents={updateContents}/>
          {
            newPost.schedules.map((schedule, idx) => <DailyScheduleForm key={idx} newSchedule={schedule} setNewSchedule={updateSchedule} idx={idx}/>)
          }
          <div className="add-schedule">
            <Button type={'secondary'} text={'+ 일차 추가하기'} onClick={addSchedule}/>
          </div>
          <div className="bottom-button">
            <Button type={'negative'} text={'작성취소'} onClick={() => {navigate("/")}}/>
            <Button type={'positive'} text={'작성완료'} onClick={completeWirtePost}/>
          </div>
        </div>
    </DefaultLayout>
  );
};

export default PostWritePage;